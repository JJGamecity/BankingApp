# Roles and account access

## Local setup with an ignored dotenv file

Run the application normally from your IDE with the project root as the working
directory. Spring dotenv's `springboot4-dotenv` integration automatically loads
the Git-ignored `.env` beside `pom.xml`. The tracked application properties use
placeholders; `application-local.properties` contains only non-secret local settings.
No manual shell/IDE environment variables are required.

That private file contains the database password. Fill in
`BANK_SECURITY_ADMIN_USERNAME` and `BANK_SECURITY_ADMIN_PASSWORD` there to enable
your initial admin. Optional representative credentials can also go there;
otherwise use the admin-only `POST /staff` command. Leave both fields blank to
disable a configured login. Representative keys are `BANK_SECURITY_REP_USERNAME`
and `BANK_SECURITY_REP_PASSWORD`. Never force-add `.env` to Git or share it.

An explicitly selected profile such as `prod` replaces the default `local`
profile, but dotenv loading is independent of profiles. Real environment variables
take precedence. Disable dotenv with `-Dspringdotenv.enabled=false` or
`SPRINGDOTENV_ENABLED=false` on deployments that use managed environment variables.

The configured staff login has the **Bank Representative** role (internally
`BANK_REPRESENTATIVE`, with Spring Security authority `ROLE_BANK_REPRESENTATIVE`).
It can use every existing user and account command, including creating customers
and bank accounts. Spring Security provides the public login page.

| Role | Access |
| --- | --- |
| BANK_REPRESENTATIVE | User and account commands; cannot create staff |
| ADMIN | All application commands, including creating staff |
| CUSTOMER | Own profile at `/users/me`; own accounts at `/accounts/me`, `/accounts/me/{accountId}`, or `/accounts/user-{id}` |

Anyone can retrieve `/csrf` to establish a session and bootstrap login/signup CSRF protection. Customers
cannot list users, view another customer's accounts, or create/update users or
accounts. Their database ID is attached to the authenticated principal and checked
before the account controller runs. Roles cannot be supplied in a customer request.

`GET /accounts/me` derives the owner from the authenticated customer principal,
without accepting an owner ID. It returns account numbers, routing numbers, types,
and available balances using the existing account response DTO. Staff identities
use the ID-based endpoint instead. The customer endpoint also checks its role at
the controller method and retains Spring Security's no-store response headers.

`GET /users/me` returns the authenticated customer's profile DTO without passwords.
`GET /accounts/me/{accountId}` queries by both account ID and authenticated owner
ID, and its service method also checks the customer role and principal ID.
Unowned and nonexistent accounts both return `404`; invalid IDs return `400`.
Customer account lists are ordered by account ID. No customer write permissions
are added by these endpoints.

Customers sign in with a record created by staff or public `POST /auth/signup`.
Public signup always creates a CUSTOMER, requires names and a DOB showing an age of at least 18, and never
accepts a credit score or privileged role. New and replaced passwords are stored as BCrypt hashes. Existing plaintext
passwords must be reset through the staff user replacement command before customer
login will work; they are not accepted as a fallback. Staff usernames take
precedence over customer usernames. New staff and customer creation/replacement
rejects usernames already used by another login or reserved by configuration.

To enable a separate ADMIN login, configure both `bank.security.admin.username`
and `bank.security.admin.password`, or set the environment variables
`BANK_SECURITY_ADMIN_USERNAME` and `BANK_SECURITY_ADMIN_PASSWORD` before starting
the app. There is no default admin password. Supplying only one setting, or reusing
the representative username, fails startup with a configuration error.

The staff login uses the existing `spring.security.user.name` and
`spring.security.user.password` settings. You can override these without editing
files using `SPRING_SECURITY_USER_NAME` and `SPRING_SECURITY_USER_PASSWORD`
environment variables. The password is BCrypt-encoded when the in-memory staff
login is created. These configured logins remain in memory for initial access.
Additional staff logins are stored in PostgreSQL's `staff_accounts` table;
customer logins use the existing `users` table. Staff roles are stored as strings
and passwords as BCrypt hashes. Flyway manages versioned schema changes and
Hibernate uses `ddl-auto=validate`; startup does not recreate tables.

## Create a staff login

As an ADMIN, send `POST http://localhost:5050/staff`:

```json
{
  "username": "jane.rep",
  "password": "replace-with-a-unique-password",
  "role": "BANK_REPRESENTATIVE"
}
```

Use `"role": "ADMIN"` to create another administrator. `CUSTOMER` is not an
accepted staff role; create customers through `POST /users`. Usernames must have
3–50 letters, digits, dots, underscores, or hyphens. Passwords must have at least
10 characters, include a special character (punctuation or a symbol, not just
whitespace), and contain no ascending/descending sequence of three or more digits
or English letters, ignoring letter case and intervening whitespace. Examples of
rejected sequences include `123`, `321`, `abc`, and `1 2 3`. Passwords are limited
to 72 UTF-8 bytes for BCrypt. First and last names are permitted if all other
rules are met. A shared `@ValidPassword` constraint enforces this policy on staff
creation and customer creation/replacement, with requirements in validation errors
and Swagger. Existing hashes still work; configured initial logins are not signup
requests and are not checked by this request validation policy.

The response is `201 Created` with `id`, `username`, and `role`; it never includes
the password or hash. Duplicate/reserved names return `409`; invalid fields or
roles return `400`. Representatives and customers receive `403`, even if they
submit `"role": "ADMIN"`. Newly created staff can immediately log in with their
own credentials. Multiple staff accounts can coexist.

Configure the first admin using the `.env` entries above, then use that
login to create additional database-backed admins. The configured admin remains
available while those settings remain configured. No staff password reset,
deletion, or role-change endpoint is included yet.

## Configuration and hardening

Passwords are no longer stored as literals in the tracked `application.properties`.
Local credentials are configured in the ignored root `.env`
described above. Standard Spring environment overrides remain optional, not
required. Do not commit real values. Removing secrets from the tracked file does not erase
older Git history; rotate any credentials previously shared or committed.

The app now binds to localhost by default. Only change `server.address` when you
intend to expose it on another interface. SQL values and parameter tracing are
disabled. Staff creation is ADMIN-only at both the HTTP and service layers.
Unconfigured endpoints are denied, and Swagger is restricted to admins.

Staff creation records the actor and timestamp in `staff_accounts.created_by`
and `created_at`, in the same transaction as the account. Older rows may have null
values. This is creation attribution, not an immutable or comprehensive audit log.

Credential-bearing requests (form login, public signup, and every HTTP Basic request) are limited
to 30 per remote IP per minute. Exceeding the limit returns 429 and `Retry-After`.
Use browser/session authentication for ongoing work. The limit can be adjusted
with `bank.security.login-attempts-per-minute`. It is held in bounded process memory,
resets on restart, and does not coordinate multiple instances. It ignores incoming
forwarding headers; behind a proxy, configure a trusted edge rate limiter instead
of trusting arbitrary `X-Forwarded-For` values.

Sessions expire after 15 idle minutes, use HttpOnly and SameSite=Lax cookies,
rotate their ID at login, and are invalidated on logout. CSRF remains enabled.
These follow Spring Security's [session management guidance](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).

## Google signup

Google OAuth is opt-in (`bank.security.google.enabled=true`) with the client ID
and secret in the ignored `.env` file for development. Spring's OIDC
login flow handles authorization state, nonce, token signature, issuer and audience
validation. Only verified Google emails and current ID tokens are accepted. The
application stores the stable Google subject in a unique `users.google_subject`
column and does not automatically link accounts by matching email.

A Google login initially receives only `GOOGLE_PENDING`, allowing `/auth/me`, the CSRF
endpoint and required signup form, without customer or staff banking access. Completing the form
requires username, first name, last name and a DOB showing an age of at least 18; untrusted form data cannot
set email, Google subject, password, role or credit score. A CUSTOMER session is
issued only after the database save succeeds, with session ID rotation and a new
CSRF token. Google-only accounts cannot authenticate with a local password.
Returning users are looked up by Google subject. Google signup never creates staff
or bank accounts. Credit score starts unset; DOB is self-reported, not identity
verification. All rendered Google/form values are HTML-escaped.

SameSite=Lax is required for the top-level GET callback from Google to carry the
application session. CSRF protection remains enabled for profile completion and
all other writes. Production uses Secure cookies and HTTPS. Fast tests simulate
verified principals; PostgreSQL integration tests additionally exercise the HTTP
code exchange and RSA-signed tokens against a local OIDC provider. A real Google
round trip requires your registered OAuth client and a manual browser smoke test.

Flyway V1 adds the Google identity column and keeps `password` and `credit_score`
nullable for Google-only customers. V2 enforces case-insensitive email uniqueness
and a database-backed username namespace shared by customers and staff. Conflicting
legacy records stop migration for review; migrations do not silently merge identities.
See the README migration instructions before adopting an existing production schema.

## Production profile

Provision and test the schema and create a database ADMIN before enabling
`SPRING_PROFILES_ACTIVE=prod`. Configured in-memory staff logins are disabled in
this profile; the database credentials are the way to sign in. The profile requires
TLS with `BANK_TLS_KEYSTORE` (PKCS12 keystore location) and
`BANK_TLS_KEYSTORE_PASSWORD`, sets Secure session cookies, disables Swagger, and
uses Flyway migrations followed by Hibernate schema validation, as local development
does. Flyway V1 includes the staff creator/timestamp columns. Baseline-on-migrate is
off by default; the tracked local profile opts in at version 0 for schema adoption.

The production profile is a starting point, not enterprise certification. Before
using real banking data, integrate an identity provider with MFA, distributed
rate limits, reviewed migration deployment, a least-privilege database account,
centralized tamper-resistant audit logs, credential rotation/recovery, and tested
backups. Those systems have not been configured by this change.

For a browser, sign in at `http://localhost:5050/login`. API clients can use HTTP
Basic authentication with the same credentials.

CSRF protection remains enabled. Before sending POST, PUT, PATCH, or DELETE:

1. Send `GET /csrf` and retain its session cookie; authentication is not required.
2. Read `headerName` and `token` from the JSON response.
3. Send that header and token, the same cookie, and authentication with the write
   request. Fetch a fresh token after logging in or out.

For example, in PowerShell with the credential environment variables set:

```powershell
$credential = [pscredential]::new(
    $env:SPRING_SECURITY_USER_NAME,
    (ConvertTo-SecureString $env:SPRING_SECURITY_USER_PASSWORD -AsPlainText -Force)
)
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
    "{0}:{1}" -f $credential.UserName, $credential.GetNetworkCredential().Password
))
$headers = @{ Authorization = "Basic $basic" }
$csrf = Invoke-RestMethod http://localhost:5050/csrf -Headers $headers -SessionVariable bankSession
$headers[$csrf.headerName] = $csrf.token
# Use -Headers $headers -WebSession $bankSession for subsequent API requests.
Invoke-RestMethod http://localhost:5050/users -Headers $headers -WebSession $bankSession
```

To create staff using the PowerShell example, use `BANK_SECURITY_ADMIN_USERNAME`
and `BANK_SECURITY_ADMIN_PASSWORD` for the credential, fetch `/csrf`, then send:

```powershell
$body = @{
    username = "jane.rep"
    password = "replace-with-a-unique-password"
    role = "BANK_REPRESENTATIVE"
} | ConvertTo-Json
Invoke-RestMethod http://localhost:5050/staff -Method Post `
    -Headers $headers -WebSession $bankSession -ContentType "application/json" -Body $body
```
