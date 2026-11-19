export const API_URL = import.meta.env.VITE_API_URL ||
  (import.meta.env.DEV ? `${window.location.protocol}//${window.location.hostname}:5050` : "/api");
let csrfToken = null;

export function resetCsrf() {
  csrfToken = null;
}

export async function request(
  path,
  { method = "GET", body, form = false, signal } = {},
) {
  const headers = { Accept: "application/json" };
  if (method !== "GET") {
    csrfToken ??= await request("/csrf");
    headers[csrfToken.headerName] = csrfToken.token;
    if (body)
      headers["Content-Type"] = form
        ? "application/x-www-form-urlencoded"
        : "application/json";
  }

  let response;
  // A first request can wake both the Java container and the database.
  const timeout = AbortSignal.timeout(60000);
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      credentials: "include",
      signal: signal ? AbortSignal.any([signal, timeout]) : timeout,
      body: body
        ? form
          ? new URLSearchParams(body)
          : JSON.stringify(body)
        : undefined,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error(
      "We cannot connect to your bank right now. Please try again in a moment.",
    );
  }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (data.errorCode === "INVALID_CSRF_TOKEN") resetCsrf();
    const error = new Error(
      data.errors?.join("\n") ||
        data.detail ||
        "Something went wrong. Please try again.",
    );
    error.status = response.status;
    throw error;
  }
  return data;
}

export const money = (value) =>
  value == null
    ? "Not available"
    : new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(value);
export { accountName } from "./formatters.js";
