# Roles and account access

## Local setup without environment variables

Run the application normally from your IDE. `application.properties` sets the
default Spring profile to `local`, which automatically loads the Git-ignored
`src/main/resources/application-local.properties`. No environment variables or
manual profile selection are required for local development.

That private file contains the database password. Fill in
`bank.security.admin.username` and `bank.security.admin.password` there to enable
your initial admin. Optional representative credentials can also go there;
otherwise use the admin-only `POST /staff` command. Leave both fields blank to
disable a configured login. Never force-add this file to Git or share it.

An explicitly selected profile such as `prod` replaces the default `local`
profile, so it does not automatically load the local credentials.

The configured staff login has the **Bank Representative** role (internally
`BANK_REPRESENTATIVE`, with Spring Security authority `ROLE_BANK_REPRESENTATIVE`).
It can use every existing user and account command, including creating customers
and bank accounts. Spring Security provides the public login page.

| Role | Access |
| --- | --- |
| BANK_REPRESENTATIVE | User and account commands; cannot create staff |
| ADMIN | All application commands, including creating staff |
| CUSTOMER | Only `GET /accounts/user-{id}` where `{id}` is their own database user ID |

All signed-in users can retrieve `/csrf` for authentication support. Customers
cannot list users, view another customer's accounts, or create/update users or
accounts. Their database ID is attached to the authenticated principal and checked
before the account controller runs. Roles cannot be supplied in a customer request.

Customers sign in with the username and password of a customer record created by
staff. New and replaced passwords are stored as BCrypt hashes. Existing plaintext
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
and passwords as BCrypt hashes. `ddl-auto=update` replaces the old `create` setting
so restarting the application no longer recreates its tables.

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
15 characters and at most 72 UTF-8 bytes. The same password length limits apply
when creating or replacing customers. Existing hashes still work; the new policy
applies when setting passwords.

The response is `201 Created` with `id`, `username`, and `role`; it never includes
the password or hash. Duplicate/reserved names return `409`; invalid fields or
roles return `400`. Representatives and customers receive `403`, even if they
submit `"role": "ADMIN"`. Newly created staff can immediately log in with their
own credentials. Multiple staff accounts can coexist.

Configure the first admin using the environment variables above, then use that
login to create additional database-backed admins. The configured admin remains
available while those settings remain configured. No staff password reset,
deletion, or role-change endpoint is included yet.

## Configuration and hardening

Passwords are no longer stored as literals in the tracked `application.properties`.
Local credentials are configured in the ignored `application-local.properties`
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

Credential-bearing requests (form login and every HTTP Basic request) are limited
to 30 per remote IP per minute. Exceeding the limit returns 429 and `Retry-After`.
Use browser/session authentication for ongoing work. The limit can be adjusted
with `bank.security.login-attempts-per-minute`. It is held in bounded process memory,
resets on restart, and does not coordinate multiple instances. It ignores incoming
forwarding headers; behind a proxy, configure a trusted edge rate limiter instead
of trusting arbitrary `X-Forwarded-For` values.

Sessions expire after 15 idle minutes, use HttpOnly and SameSite=Strict cookies,
rotate their ID at login, and are invalidated on logout. CSRF remains enabled.
These follow Spring Security's [session management guidance](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).

## Production profile

Provision and test the schema and create a database ADMIN before enabling
`SPRING_PROFILES_ACTIVE=prod`. Configured in-memory staff logins are disabled in
this profile; the database credentials are the way to sign in. The profile requires
TLS with `BANK_TLS_KEYSTORE` (PKCS12 keystore location) and
`BANK_TLS_KEYSTORE_PASSWORD`, sets Secure session cookies, disables Swagger, and
validates the schema instead of automatically modifying it. Local development
still uses `ddl-auto=update`.

For an existing staff table, a reviewed PostgreSQL migration must include:

```sql
ALTER TABLE staff_accounts ADD COLUMN IF NOT EXISTS created_by varchar(255);
ALTER TABLE staff_accounts ADD COLUMN IF NOT EXISTS created_at timestamp(6) with time zone;
```

The production profile is a starting point, not enterprise certification. Before
using real banking data, integrate an identity provider with MFA, distributed
rate limits, managed schema migrations, a least-privilege database account,
centralized tamper-resistant audit logs, credential rotation/recovery, and tested
backups. Those systems have not been configured by this change.

For a browser, sign in at `http://localhost:5050/login`. API clients can use HTTP
Basic authentication with the same credentials.

CSRF protection remains enabled. Before sending POST, PUT, PATCH, or DELETE:

1. Send an authenticated `GET /csrf` and retain its session cookie.
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
