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
user needs permission to create/update application tables. Hibernate updates the
local schema on startup; it does not recreate the tables each time.

### 2. Configure private local credentials

Open `src/main/resources/application-local.properties`. If you just cloned this
repository, create that file: it is intentionally excluded from Git.

```properties
# Use the existing password for your PostgreSQL database user.
spring.datasource.password=REPLACE_WITH_YOUR_DATABASE_PASSWORD

# Initial admin used to create additional staff logins.
bank.security.admin.username=initial.admin
bank.security.admin.password=REPLACE_WITH_A_UNIQUE_LONG_PASSWORD
```

Replace the example values with your own. Do not overwrite a valid database
password already present in your local file. These settings do not change the
PostgreSQL user's password.

**No environment variables are required for local development.** The main
`application.properties` selects `local` as the default profile, so this private
file loads automatically. Keep real passwords out of tracked files and never
force-add the local file to Git.

If your database name, username, or port differs, override
`spring.datasource.url` and `spring.datasource.username` in the same local file.

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

## Roles

| Role | Permissions |
| --- | --- |
| `ADMIN` | All implemented user/account commands and staff creation |
| `BANK_REPRESENTATIVE` | Create, view, and update customers and bank accounts; cannot create staff |
| `CUSTOMER` | View only accounts belonging to their authenticated database user ID |

Customers cannot grant themselves a staff role. Staff creation is checked at both
the HTTP and service layers. Unknown endpoints are denied by default.

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
New staff and customer passwords must be at least 15 characters and at most 72
UTF-8 bytes. Passwords are BCrypt-hashed before being stored.

Duplicate or reserved usernames return `409`; invalid input returns `400`.
Representatives and customers receive `403` when attempting staff creation.

## Create a customer and their bank account

As an admin or Bank Representative, send `POST /users` with the same authentication,
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

## API endpoints

Paths below are relative to `http://localhost:5050`.

| Method | Path | Purpose | Access |
| --- | --- | --- | --- |
| `POST` | `/staff` | Create an admin or representative | Admin |
| `POST` | `/users` | Create a customer | Admin / representative |
| `GET` | `/users` | List customers | Admin / representative |
| `GET` | `/users/{id}` | Read a customer | Admin / representative |
| `PUT` | `/users/{id}` | Update a customer | Admin / representative |
| `POST` | `/accounts` | Create a bank account | Admin / representative |
| `PUT` | `/accounts?id={accountId}` | Update a bank account | Admin / representative |
| `GET` | `/accounts/user-{id}` | List a customer's accounts | Admin / representative / owning customer |
| `GET` | `/csrf` | Retrieve a CSRF token | Any authenticated user |
| `GET` | `/login` | Browser login page | Public |
| `POST` | `/logout` | End the browser session | Valid CSRF token required |

The update endpoints accept the corresponding user/account request body. There
are currently no HTTP endpoints for deleting users/accounts or resetting staff
passwords, deleting staff, or changing staff roles.

## Where data is stored

- Customers: PostgreSQL `users` table.
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
- Credential-bearing requests are limited to 30 per remote IP per minute,
  including every Basic Auth request. A `429` response includes `Retry-After`.
  Browser/session authentication avoids resending credentials on every request.
- Sessions expire after 15 idle minutes. Login rotates the session ID; logout
  invalidates it. Cookies use HttpOnly and SameSite=Strict.
- The application binds to localhost by default, and SQL parameter logging is
  disabled. Creation attribution is not a comprehensive audit trail.

See [SECURITY.md](SECURITY.md) for configuration and production setup. The `prod`
profile requires TLS and a provisioned schema, disables configured in-memory
staff logins, and uses database-backed staff authentication. Create a database
admin before switching to it. Selecting `prod` does not automatically load the
default local profile or its private credentials.

This is a development project, not a production-ready banking system. MFA/SSO,
distributed rate limiting, managed migrations, centralized audit logs, and
operational recovery controls still need to be implemented for real deployment.

## Tests

Run the test suite from IntelliJ or Maven:

```powershell
.\mvnw.cmd test
```

The suite covers roles, customer account ownership, staff creation, password
hashing, username conflicts, CSRF, rate limiting, browser sessions, and logout.
Repository-backed flows use mocked repositories; these tests do not verify a
live PostgreSQL database or TLS deployment.
