import { accountName } from "./formatters.js";

export function bankingCommands(roles, accounts = []) {
  const customer = roles.includes("CUSTOMER");
  const pages = customer
    ? [
        {
          id: "overview",
          title: "Your overview",
          detail: "Balances, Money Compass, and credit score",
          page: "overview",
          keywords: "dashboard home credit score compass",
        },
        {
          id: "accounts",
          title: "My accounts",
          detail: "Explore your connected accounts",
          page: "accounts",
          keywords: "balance checking savings money market",
        },
        {
          id: "profile",
          title: "My profile",
          detail: "Your personal information and credit score",
          page: "profile",
          keywords: "name email personal credit score",
        },
      ]
    : [
        {
          id: "customers",
          title: "Find a customer",
          detail: "Open customer management",
          page: "overview",
          keywords: "users customers search",
        },
        {
          id: "open",
          title: "Open an account",
          detail: "Go to the account-opening form",
          page: "open",
          keywords: "new create checking savings",
        },
        ...(roles.includes("ADMIN")
          ? [
              {
                id: "staff",
                title: "Create staff",
                detail: "Go to staff registration",
                page: "staff",
                keywords: "employee admin new register",
              },
            ]
          : []),
      ];
  return [
    ...pages.map((page) => ({ ...page, group: "Workspace" })),
    ...(customer
      ? accounts.map((account) => ({
          id: `account-${account.id}`,
          title: accountName(account.accountType),
          detail: `Account ending ${String(account.accountNumber).slice(-4)}`,
          accountId: account.id,
          group: "Your accounts",
          keywords: String(account.accountNumber).slice(-4),
        }))
      : []),
  ];
}

export function searchCommands(commands, query) {
  const words = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
  return commands.filter((command) =>
    words.every((word) =>
      `${command.title} ${command.detail} ${command.keywords}`
        .toLowerCase()
        .includes(word),
    ),
  );
}
