export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:5050";
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
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      credentials: "include",
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(10000)])
        : AbortSignal.timeout(10000),
      body: body
        ? form
          ? new URLSearchParams(body)
          : JSON.stringify(body)
        : undefined,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error(
      "We cannot connect to your bank right now. Check that BankingApp is running on port 5050 and try again.",
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
export const accountName = (value) =>
  String(value || "")
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
