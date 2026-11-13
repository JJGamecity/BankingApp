# Banking App

A Spring Boot REST API for managing customers, bank accounts, and staff logins.
Built with Java 21, Spring Security, Spring Data JPA, PostgreSQL, and Maven.

## Run locally

### 1. Prepare PostgreSQL

Install Java 21 and run PostgreSQL. The default connection settings are:

| Setting | Default |
| --- | --- |
| Host and port | `localhost:5432` |
| Database | `bankingData` |
| Database user | `bankAppAdmin` |
| Application address | `http://localhost:5050` |

Create the database and database user if they do not exist. The local database
user needs permission to run schema migrations. Flyway applies versioned migrations
on startup, then Hibernate validates the schema. Existing records are retained.

### 2. Configure private local credentials

Open `.env` in the project root (beside `pom.xml`). If you just cloned this
repository, copy `.env.example` to `.env` and fill in your values. `.env` is ignored
by Git; the example and all application properties files are safe to track.

```properties
# Use the existing password for your PostgreSQL database user.
BANK_DB_PASSWORD="REPLACE_WITH_YOUR_DATABASE_PASSWORD"

# Initial admin used to create additional staff logins.
BANK_SECURITY_ADMIN_USERNAME=initial.admin
BANK_SECURITY_ADMIN_PASSWORD="REPLACE_WITH_A_UNIQUE_LONG_PASSWORD"
```

Replace the example values with your own. Do not overwrite a valid database
password already present in your `.env`. These settings do not change the
PostgreSQL user's password.

**No manual shell or IDE environment-variable setup is required.** Spring dotenv's
Boot 4 integration (`me.paulschwarz:springboot4-dotenv:5.1.0`) loads `.env` automatically
from the working directory. Set the IDE working directory to this project root.
`application.properties` maps the values through `${BANK_...}` placeholders;
`application-local.properties` contains only safe local migration settings.
Never force-add `.env` to Git. It remains outside application resources and the built JAR.

If your database name, username, or port differs, override
`BANK_DB_URL` and `BANK_DB_USERNAME` in `.env`. Optional representative credentials use
`BANK_SECURITY_REP_USERNAME` and `BANK_SECURITY_REP_PASSWORD`. Real environment variables
and explicit command-line settings take precedence over `.env`.

### 3. Start the app

In IntelliJ, use Java 21, import the Maven project, and run
`BankingAppApplication` with the Run button. Leave active profiles unset for the
default local configuration.

Alternatively, use the Maven wrapper:

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```sh
# macOS / Linux
./mvnw spring-boot:run
```

Open [the login page](http://localhost:5050/login) and enter your initial admin
credentials. The app provides REST responses, not a complete banking dashboard.
An admin can also view [Swagger UI](http://localhost:5050/swagger-ui/index.html).

## Frontend authentication and public signup

Use session cookies for the frontend. Default allowed frontend origins are
`http://localhost:5173` and `http://127.0.0.1:5173`. To change them, put a comma-separated
`bank.security.frontend-origins` value in `application-local.properties` (this non-secret setting is tracked).
Only list trusted exact origins; wildcards are rejected. Use the same hostname
for the frontend and API (for example, `localhost` for both). A same-origin proxy
also works. Production frontend/API hosting must suit the SameSite=Lax cookie policy.

1. `GET /csrf` with `credentials: "include"` before logging in or signing up.
   This endpoint is public and returns `headerName`, `parameterName`, and `token`.
2. `POST /login` with form-encoded `username` and `password`, `Accept: application/json`,
   the CSRF header, and the same session cookie. Success is `204` with no redirect;
   wrong credentials return `401` with `errorCode: "INVALID_CREDENTIALS"`.
3. Fetch a **fresh** `/csrf` token after login, since authentication rotates it.
4. `GET /auth/me` returns the current username, customer ID (null for staff), roles,
   and `signupRequired`. This works for ADMIN, BANK_REPRESENTATIVE, CUSTOMER, and
   pending Google signup. Pending Google users have a null username until completion.
5. Send the CSRF header and cookie with every write. `POST /logout` returns `204`
   and invalidates the session; fetch a new token before another login.

Protected API requests without a valid session return `401` / `AUTHENTICATION_REQUIRED`.
The frontend should show its login screen. Insufficient permission returns
`403` / `ACCESS_DENIED`; missing or stale CSRF returns `403` / `INVALID_CSRF_TOKEN`.
The existing HTML `/login` page and HTTP Basic clients remain supported.

Example frontend login:

```javascript
const api = "http://localhost:5050";
const options = { credentials: "include" };
let csrf = await (await fetch(`${api}/csrf`, options)).json();
const response = await fetch(`${api}/login`, {
  ...options,
  method: "POST",
  headers: {
    Accept: "application/json",
    "Content-Type": "application/x-www-form-urlencoded",
    [csrf.headerName]: csrf.token,
  },
  body: new URLSearchParams({ username, password }),
});
if (!response.ok) throw await response.json();
csrf = await (await fetch(`${api}/csrf`, options)).json();
const session = await (await fetch(`${api}/auth/me`, options)).json();
```

For public password signup, send `POST /auth/signup` with JSON, a CSRF token,
and its session cookie:

```json
{
  "email": "james@example.com",
  "username": "james.smith",
  "password": "James!90758",
  "firstName": "James",
  "lastName": "Smith",
  "dateOfBirth": "1995-06-15"
}
```

Use your own password; the example illustrates the format. The existing password
requirements apply: at least 10 characters, a special character, no ascending or
descending three-character letter/digit sequences (even separated by whitespace),
and at most 72 UTF-8 bytes. Names are allowed. First/last names and a past DOB are
required, and you must be **at least 18 years old**; usernames use 3–15 letters,
digits, dots, underscores, or hyphens. Age is checked by calendar date on the server;
signup is accepted on the eighteenth birthday. A February 29 birth date reaches
the age cutoff on March 1 in a non-leap year.

Invalid signup fields return `400` with `errorCode: "VALIDATION_ERROR"` and an
`errors` list containing clear field messages, for example:

```json
{
  "status": 400,
  "title": "Validation Failed",
  "detail": "One or more fields have errors.",
  "errorCode": "VALIDATION_ERROR",
  "errors": ["dateOfBirth: You must be at least 18 years old to sign up."]
}
```

Missing names, invalid emails/usernames, and password failures include their own
messages in that list. Invalid dates explain the `YYYY-MM-DD` format. Duplicate
emails return `409` / `SIGNUP_CONFLICT` with a sign-in suggestion in `detail`.

Success returns `201` and the customer profile, without a password. It does not
automatically log in or open a bank account; use `/login` next. Every public signup
is a CUSTOMER, with an initially null credit score. Submitted role, credit score,
or Google identity fields cannot grant privileges. Duplicate usernames/emails return
`409`; email uniqueness is case insensitive. Bank representatives can still use `POST /users`.

## Sign up with Google

Google signup works in a browser without a separate frontend. It is disabled by
default until you supply OAuth credentials from your Google Cloud project.
The frontend checks public `GET /auth/options` and enables its Google button only
when `googleSignInEnabled` is true. If it is false, configure the three `.env`
entries below, restart the backend, and refresh the frontend. The endpoint exposes
only availability, never OAuth credentials. Staff sessions must sign out before
starting a separate customer login.

1. In Google Cloud's Google Auth Platform, configure your application's audience
   and consent information, then create an **OAuth client for a Web application**.
   If the application is in testing mode, add your Google account as a test user.
2. Add this exact **authorized redirect URI**:
   `http://localhost:5050/login/oauth2/code/google`.
3. Fill in the following entries in the ignored project-root `.env` and enable the feature:

```properties
BANK_SECURITY_GOOGLE_ENABLED=true
BANK_SECURITY_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
BANK_SECURITY_GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
```

4. Restart the app. Open `http://localhost:5050/login` and choose Google, or visit
   `http://localhost:5050/oauth2/authorization/google` directly. Use the same
   `localhost` hostname throughout the login flow.
5. After Google verifies the login, a new customer must submit the profile form
   at `/signup/google`: **username, first name, last name, and date of birth**.
   Names may be prefilled from Google but must be confirmed by submitting the form.
   DOB is required and the customer must be **at least 18**; it is entered by the customer, not
   supplied or verified by Google. The username must be 3–15 letters, digits,
   dots, underscores, or hyphens.

Until the form is complete, the login has no access to banking commands. On
completion, the app creates a CUSTOMER profile, stores the supplied name and DOB
in `users`, and redirects to `/accounts/me`. No bank account is opened automatically.
The credit score is initially null until staff assess it. Google signup does not
ask for a local password; Google-only accounts must use Google to log in.

Returning Google users are matched using Google's stable subject identifier and
go directly to their own accounts. Email matches never automatically link an
existing customer or staff account; if your email is already registered, use your
existing login and contact the bank. Google cannot create staff roles.

For deployment, register the exact HTTPS callback URL for that deployment and
configure its client credentials separately. Dotenv is independent of Spring profiles:
if `.env` is present in the deployment working directory, it can also load there.
For deployments using managed environment variables, disable dotenv with
`-Dspringdotenv.enabled=false` or `SPRINGDOTENV_ENABLED=false`.
See Google's [OpenID Connect guide](https://developers.google.com/identity/openid-connect/openid-connect)
for OAuth client setup and redirect URI requirements.

## Roles

| Role | Permissions |
| --- | --- |
| `ADMIN` | Create and list staff; administrative documentation. No customer or bank-account access |
| `BANK_REPRESENTATIVE` | Manage customers and their bank accounts; no personal customer account/signup or staff management |
| `CUSTOMER` | View their own profile and accounts using their authenticated database user ID |

Customers cannot grant themselves a staff role. Staff creation is checked at both
the HTTP and service layers. Unknown endpoints are denied by default.

Admins and bank representatives are staff identities, with no customer ID or
personal bank-account ownership. Neither staff role can use customer signup
(including Google) or personal `/users/me` and `/accounts/me` endpoints. Representatives
can manage customers' bank accounts through the staff commands; admins cannot.
Customer/account services also reject ADMIN access.
After browser login, admins land on `GET /staff`, which lists database-backed staff
IDs, usernames, and roles without password hashes. Configured bootstrap logins are
in memory and are not included in that list. Representatives continue to manage
customers and bank accounts. Every role retains `/auth/me`, CSRF, and logout access.

## Create staff using Postman

1. Send `GET http://localhost:5050/csrf`. In **Authorization > Basic Auth**, use
   your initial admin's username and password.
2. Copy the response's `token`. Keep the session cookie Postman receives.
3. Create a `POST http://localhost:5050/staff` request with the same Basic Auth
   credentials and session cookie.
4. Add the `X-CSRF-TOKEN` header with the copied token. Select **Body > raw > JSON**
   and send:

```json
{
  "username": "jane.rep",
  "password": "REPLACE_WITH_A_UNIQUE_LONG_PASSWORD",
  "role": "BANK_REPRESENTATIVE"
}
```

To create another admin, choose a different username/password and set `role` to
`ADMIN`. A successful request returns `201 Created` with the staff ID, username,
and role. The new credentials can be used immediately.

Staff usernames must be 3–50 letters, digits, dots, underscores, or hyphens.
New customer and staff passwords must meet these requirements:

- At least **10 characters**.
- At least **one special character**, such as `!`, `@`, `#`, or `-`. Spaces alone do not count.
- No ascending or descending sequence of **3 or more digits or English letters**,
  such as `123`, `321`, `abc`, or `cba`. Letter matching ignores case, and spaces
  between sequence characters do not bypass this rule (`1 2 3` is also rejected).
- At most **72 UTF-8 bytes**, the BCrypt input limit.

**First and last names are allowed** as long as the password meets the other
requirements. For example, `James!Lane` illustrates an accepted 10-character
password; choose your own unique password rather than using this example.
`James123!Lane` is rejected because it contains `123`.

These rules apply to customer signup, customer replacement, and staff creation.
Invalid passwords return `400` with the requirements in the `password` field error;
the same requirements appear in Swagger's password field description. Existing
logins are not retroactively rejected by this policy. Passwords remain BCrypt-hashed.

Duplicate or reserved usernames return `409`; invalid input returns `400`.
Representatives and customers receive `403` when attempting staff creation.

## Create a customer and their bank account

As a Bank Representative, send `POST /users` with the same authentication,
CSRF header, and session-cookie setup:

```json
{
  "email": "jane@example.com",
  "username": "jane.customer",
  "password": "REPLACE_WITH_A_UNIQUE_LONG_PASSWORD",
  "dateOfBirth": "1995-06-15",
  "firstName": "Jane",
  "lastName": "Smith",
  "creditScore": 700
}
```

Customer usernames must be 3–15 characters; credit scores must be between 300 and
850. Use the returned customer `id` as `userId` in `POST /accounts`:

```json
{
  "userId": 1,
  "accountNumber": "1000000001",
  "accountType": "CHECKING"
}
```

Replace `1` with the actual customer ID. Supported account types are `CHECKING`,
`SAVINGS`, and `MONEY_MARKET`. The customer can then log in and read
`GET /accounts/user-1` for their own ID. Changing the ID to another customer's
returns `403`.

## View your own accounts as a customer

Sign in at `http://localhost:5050/login` using a customer's username and password.
The browser redirects to `GET /accounts/me`, which returns that customer's accounts.
In Postman, send **GET** `http://localhost:5050/accounts/me` with the customer's
credentials under **Authorization > Basic Auth**. No body or CSRF token is needed
for this read-only request.

Example response (illustrative values):

```json
[
  {
    "id": 10,
    "userId": 1,
    "accountType": "CHECKING",
    "accountNumber": "1000000001",
    "routingNumber": "91000019",
    "availableBalance": 1250.75
  }
]
```

The response includes all of the signed-in customer's accounts. A customer without
accounts receives `[]`. The owner is determined by the authenticated identity;
supplying a different `userId` or `id` query parameter cannot select another owner.
The response does not include login credentials and is marked `no-store`.

This endpoint is for customer identities. Bank representatives use
`GET /accounts/user-{id}` when viewing customer accounts.

Customers can also send these GET requests with the same login:

| Path | Response |
| --- | --- |
| `/users/me` | Own ID, username, email, first name, last name, and credit score; no password |
| `/accounts/me` | All owned accounts, ordered by account ID; `[]` when none exist |
| `/accounts/me/10` | One owned account, including its balance, account number, and routing number |

Replace `10` with an account ID returned by `/accounts/me`. Account IDs must be
positive integers. Invalid IDs return `400`. An account that is missing or belongs
to someone else returns the same `404` response, without revealing its owner.
These endpoints are read-only and customer-only. The existing
`/accounts/user-{id}` endpoint remains available with its ownership checks.

## API endpoints

Paths below are relative to `http://localhost:5050`.

| Method | Path | Purpose | Access |
| --- | --- | --- | --- |
| `POST` | `/staff` | Create an admin or representative | Admin |
| `GET` | `/staff` | List database-backed staff without credentials | Admin |
| `POST` | `/users` | Create a customer | Representative |
| `GET` | `/users` | List customers | Representative |
| `GET` | `/users/{id}` | Read a customer | Representative |
| `GET` | `/users/me` | View own customer profile | Customer |
| `PUT` | `/users/{id}` | Update a customer | Representative |
| `POST` | `/accounts` | Create a bank account | Representative |
| `PUT` | `/accounts?id={accountId}` | Update a bank account | Representative |
| `GET` | `/accounts/me` | View own accounts, balances, and bank details | Customer |
| `GET` | `/accounts/me/{accountId}` | View one owned account | Customer |
| `GET` | `/accounts/user-{id}` | List a customer's accounts | Representative / owning customer |
| `GET` | `/csrf` | Retrieve a CSRF token and establish a session | Public |
| `GET` | `/auth/me` | Current username, customer ID, roles, and signup status | Any authenticated user |
| `GET` | `/auth/options` | Whether Google sign-in is enabled; no credentials | Public |
| `POST` | `/auth/signup` | Create a password-based CUSTOMER profile | Public + CSRF; staff sessions denied |
| `POST` | `/login` | Form-encoded credentials; JSON clients receive `204` | Public + CSRF |
| `POST` | `/logout` | Invalidate the session | CSRF |
| `GET` | `/login` | Browser login page | Public |
| `GET` | `/oauth2/authorization/google` | Start Google login/signup when enabled | Public; staff sessions denied; no Authorization header |
| `GET` | `/signup/google` | Required Google profile-completion form | Incomplete Google signup |
| `POST` | `/signup/google` | Submit form-encoded username, first name, last name and DOB | Incomplete Google signup + CSRF |
| `POST` | `/logout` | End the browser session | Valid CSRF token required |

The update endpoints accept the corresponding user/account request body. There
are currently no HTTP endpoints for deleting users/accounts or resetting staff
passwords, deleting staff, or changing staff roles.

## Where data is stored

- Customers: PostgreSQL `users` table.
- Google customer identity: unique `users.google_subject`; the same row stores
  confirmed names and DOB, with no local password and an initially unset credit score.
- Bank accounts: PostgreSQL `account` table (the default mapping for the `Account` entity).
- Staff created through `/staff`: PostgreSQL `staff_accounts` table, including
  their password hash, role, creator, and creation timestamp.
- Initial admin from local configuration: in memory at startup, not a database
  row. It remains available while its configuration is enabled.

New staff accounts survive application restarts. Existing customer passwords
stored as plaintext before authentication was added must be reset through the
staff customer-update command before they can be used to log in.

## Security and troubleshooting

- Write requests require a CSRF token and its matching session cookie, even with
  Basic Auth. Fetch a fresh token after login/logout. A missing or stale token
  commonly causes `403` responses.
- Login/signup attempts are limited to 30 per remote IP per minute,
  including every Basic Auth request. A `429` / `RATE_LIMITED` response includes `Retry-After`.
  Browser/session authentication avoids resending credentials on every request.
- Sessions expire after 15 idle minutes. Login rotates the session ID; logout
  invalidates it. Cookies use HttpOnly and SameSite=Lax to allow Google's callback.
- The application binds to localhost by default, and SQL parameter logging is
  disabled. Creation attribution is not a comprehensive audit trail.

See [SECURITY.md](SECURITY.md) for configuration and production setup. The `prod`
profile requires TLS and a provisioned schema, disables configured in-memory
staff logins, and uses database-backed staff authentication. Create a database
admin before switching to it. Selecting `prod` does not automatically load the
default local profile. Dotenv loading is independent of that profile selection.

This is a development project, not a production-ready banking system. MFA/SSO,
distributed rate limiting, centralized audit logs, and
operational recovery controls still need to be implemented for real deployment.

## API errors

Controller errors use `ProblemDetail` responses with `status`, `title`, `detail`,
and `errorCode`. Validation failures also include an `errors` list; multiple
violations for the same field are preserved. Internal exception text, SQL details,
and rejected parameter values are not included in generic error descriptions.
The standard `instance` field may contain the requested URL path.

| Status | Typical cause |
| --- | --- |
| `400` | Missing body/parameter, malformed JSON, invalid ID, or failed field validation |
| `401` / `403` | Missing authentication / insufficient permission |
| `404` | Customer/account not found, or an account not owned by the customer |
| `405` | Unsupported HTTP method; `Allow` identifies supported methods |
| `406` / `415` | Unsupported response format / request Content-Type |
| `409` | Duplicate data, constraint conflict, or an optimistic update conflict |
| `503` | Database connection/resource failure, transient database error, or request timeout |
| `500` | Unexpected server error, including invalid server return values |

Account create/update bodies require a positive `userId`, a nonblank
`accountNumber` (maximum 255 characters), and `accountType`. Account updates also
require a positive `id` query parameter: `PUT /accounts?id=1`.

A `503` does not guarantee that a write failed to commit; check its outcome before
retrying. Handling optimistic-lock exceptions does not itself add optimistic
locking to entities. Errors rejected in Spring Security's filter chain retain
its authentication/CSRF responses and OAuth redirects; they do not pass through
the controller exception handler. Unknown routes can be denied by security before
MVC returns a `404`.

## Tests

Run the test suite from IntelliJ or Maven:

```powershell
.\mvnw.cmd test
```

The suite covers roles, customer account ownership, staff creation, password
hashing, username conflicts, CSRF, rate limiting, browser sessions, and logout.
Google tests also cover restricted pending sessions, required names/DOB, CSRF,
claim escaping, forged callbacks, and prevention of automatic email-based linking.
These fast tests use mocked repositories. Run the real PostgreSQL integration suite with:

```powershell
.\mvnw.cmd -Ppostgres-it verify
```

It starts isolated, password-protected PostgreSQL instances on temporary loopback
ports using your installed PostgreSQL binaries. It never connects to `bankingData`
or loads your local credentials: Maven's test runners disable dotenv, and the dotenv
configuration tests explicitly use temporary files containing fake values.
Windows defaults to `C:/Program Files/PostgreSQL/18/bin`;
elsewhere the binaries must be on PATH. Override the location with the Maven option
`"-Dbank.test.postgres-bin=C:/path/to/PostgreSQL/bin"` if needed. Test clusters/logs
remain under ignored `target/postgres-it-*` directories, and servers stop on JVM exit.

Integration tests cover signup and login/logout, roles, CSRF, CORS, account ownership,
profile edits, duplicate/concurrent registrations, fresh and legacy migrations,
and persistence across a PostgreSQL restart. Google integration tests perform a
real HTTP authorization-code exchange against a local test provider, including
RSA-signed tokens, nonce validation, profile completion, and returning login.
They do not contact Google or verify your actual Google Cloud client configuration.
A real Google browser login and TLS deployment still require a manual smoke test.

## Database migrations

Flyway runs `src/main/resources/db/migration` before Hibernate's `ddl-auto=validate`:

- V1 creates the tables on a fresh database, or adopts existing tables and adds
  the Google identity and staff attribution columns without deleting records.
- V2 enforces case-insensitive customer email uniqueness and a shared `login_names`
  registry maintained by database triggers. Concurrent customer/staff creation cannot
  claim the same username. Username matching remains case sensitive.

The tracked local profile enables baselining at version **0** so V1
also runs on the existing Hibernate-managed database. A fresh database needs no
baseline setting. When adopting another existing database, first back it up and review
its schema, then explicitly set `spring.flyway.baseline-on-migrate=true` and
`spring.flyway.baseline-version=0` for that adoption. Baseline-on-migrate is disabled
in shared defaults. If duplicate emails or cross-table usernames already exist, V2
fails rather than silently deleting or merging customer records; resolve conflicts
and restart. Existing incompatible schemas fail validation for review.

Add a new numbered migration for future schema changes; do not edit migrations
already applied to a database. No manual SQL snippets are needed for Google or staff
attribution columns after these migrations are enabled.
