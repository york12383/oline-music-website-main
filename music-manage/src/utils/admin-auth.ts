interface AdminInfo {
  username?: string;
  name?: string;
  loginTime?: number;
}

const LAST_ADMIN_USERNAME_KEY = "lastAdminUsername";

function parseAdminInfo(value: string | null): AdminInfo | null {
  if (!value) {
    return null;
  }

  try {
    const parsed = JSON.parse(value) as AdminInfo;
    if (typeof parsed?.username === "string" && parsed.username.trim()) {
      return parsed;
    }
    if (typeof parsed?.name === "string" && parsed.name.trim()) {
      return parsed;
    }
  } catch (error) {
    return null;
  }

  return null;
}

export function getStoredAdminInfo() {
  const localAdmin = parseAdminInfo(localStorage.getItem("adminInfo"));
  if (localAdmin) {
    return localAdmin;
  }

  return parseAdminInfo(sessionStorage.getItem("adminInfo"));
}

export function hasValidStoredAdminInfo() {
  return Boolean(getStoredAdminInfo());
}

export function clearStoredAdminInfo() {
  localStorage.removeItem("adminInfo");
  sessionStorage.removeItem("adminInfo");
}

export function rememberLastAdminUsername(username?: string) {
  const normalized = String(username || "").trim();
  if (!normalized) {
    localStorage.removeItem(LAST_ADMIN_USERNAME_KEY);
    return;
  }
  localStorage.setItem(LAST_ADMIN_USERNAME_KEY, normalized);
}

export function getLastAdminUsername() {
  return String(localStorage.getItem(LAST_ADMIN_USERNAME_KEY) || "").trim();
}
