import { useSyncExternalStore } from "react";

export const paths = {
  overview: "/overview",
  accounts: "/accounts",
  profile: "/profile",
  customers: "/customers",
  open: "/accounts/new",
  staff: "/staff/new",
};

export function navigate(path, replace = false) {
  if (window.location.pathname === path) return;
  window.history[replace ? "replaceState" : "pushState"]({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
  window.scrollTo(0, 0);
}

function subscribe(callback) {
  window.addEventListener("popstate", callback);
  return () => window.removeEventListener("popstate", callback);
}

export function usePath() {
  return useSyncExternalStore(subscribe, () => window.location.pathname);
}

export function followLink(event, path) {
  if (
    event.button !== 0 ||
    event.ctrlKey ||
    event.metaKey ||
    event.shiftKey ||
    event.altKey
  )
    return;
  event.preventDefault();
  navigate(path);
}
