# BankingApp frontend

React + Vite, with Lucide icons and custom CSS. All visual styling lives in src/styles.css; Tailwind is not required.

## Run

Start the Java backend in IntelliJ. From this folder:

```powershell
npm.cmd install
npm.cmd run dev
```

Open http://localhost:5173. Use localhost for both frontend and backend so session cookies work. Stop the old frontend server first if port 5173 is occupied. Use Node.js 20.19+, 22.12+, or newer.

## File organization

- index.html: HTML document, metadata, and React entry point.
- src/main.jsx: mounts React and loads CSS.
- src/App.jsx: session state and screen selection.
- src/styles.css: colors, layout, controls, and responsive rules.
- src/lib/api.js: API requests, CSRF handling, and formatting.
- src/components/UI.jsx: reusable fields, account cards, and status messages.
- src/components/Layout.jsx: navigation, sidebar, and top bar.
- src/pages/AuthPage.jsx: sign-in and signup markup.
- src/pages/CustomerPage.jsx: overview, accounts, and profile markup.
- src/pages/StaffPage.jsx: customer search, account opening, and staff creation.

React markup lives in readable JSX components rather than HTML strings. index.html is the document shell. No inline styles are used.

## Commands

- npm.cmd run dev: development server with live updates.
- npm.cmd run build: production assets in dist/.
- npm.cmd run preview: preview the production build on port 5173.
- npm.cmd run format: format source, CSS, and configuration with Prettier.

## Backend

API defaults to http://localhost:5050. Set VITE_API_URL in .env.local to override. This is public configuration; never place credentials there.

Requests use session cookies and server-provided CSRF tokens. Login/logout reset cached tokens; expired sessions return to sign-in. The existing backend permits localhost:5173 through CORS.

Customer screens load real accounts and profile data. Staff can search customers, view accounts, and open accounts. Administrators can create staff. Server authorization remains authoritative.

Google login must be configured in the backend. The frontend checks GET /auth/options and disables its Google button with an explanation when the backend has not enabled Google sign-in. Configure BANK_SECURITY_GOOGLE_ENABLED, BANK_SECURITY_GOOGLE_CLIENT_ID, and BANK_SECURITY_GOOGLE_CLIENT_SECRET in the backend's project-root .env, restart the backend, and refresh the frontend. Never put client secrets in frontend configuration. After finishing its redirect flow, return here and select Check my session. Pending signup links to the backend profile-completion page.

No fake balances or transactions are used. The welcome-page card is decorative, not an issued card. Deposits, transfers, and withdrawals are not implemented by the backend.

For deployment, serve dist/ behind HTTPS with suitable proxy, cookie, and CORS configuration. Vite preview is for local verification.

## Page URLs

/login and /signup open their matching forms. Customers use /overview, /accounts, and /profile. Staff use /customers and /accounts/new; administrators can use /staff/new. Browser Back/Forward and direct page refresh are supported. Protected links require sign-in and retain an allowed destination after login. Production hosting must rewrite frontend routes to index.html (without rewriting backend API routes).

## Visual assets

- Google G artwork: https://developers.google.com/static/identity/images/g-logo.png (official Google Identity branding asset).
- Architecture photograph: https://unsplash.com/photos/a-very-tall-building-covered-in-lots-of-green-plants-ImVu4gOig2U
- Alpine lake photograph by kodex1213: https://unsplash.com/photos/a-large-body-of-water-surrounded-by-mountains-ioZgM-tgWFI

Images are stored locally under public/images, so visiting the app does not send image requests to external providers. The gallery is manually controlled, keyboard accessible, and respects reduced-motion preferences. Signup checkmarks are guidance for length and special characters; all password rules are still validated by the backend.
