import type { LocationQuery, LocationQueryRaw, LocationQueryValue } from "vue-router";

type QueryValue = string | number | null | undefined;

function pickFirstQueryValue(
  value: LocationQueryValue | LocationQueryValue[] | null | undefined
): string {
  if (Array.isArray(value)) {
    const first = value.find((item): item is string => typeof item === "string");
    return first ?? "";
  }
  return typeof value === "string" ? value : "";
}

function normalizeQueryRecord(query: LocationQuery | LocationQueryRaw): Record<string, string> {
  const result: Record<string, string> = {};

  Object.entries(query).forEach(([key, value]) => {
    const normalized = pickFirstQueryValue(
      value as LocationQueryValue | LocationQueryValue[] | null | undefined
    );
    if (normalized !== "") {
      result[key] = normalized;
    }
  });

  return result;
}

export function normalizeQueryValue(
  value: LocationQueryValue | LocationQueryValue[] | null | undefined
): string {
  return pickFirstQueryValue(value);
}

export function parsePositiveIntQuery(
  value: LocationQueryValue | LocationQueryValue[] | null | undefined,
  fallback = 1
): number {
  const parsed = Number.parseInt(pickFirstQueryValue(value), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function mergeRouteQuery(
  currentQuery: LocationQuery,
  patch: Record<string, QueryValue>
): LocationQueryRaw {
  const nextQuery = normalizeQueryRecord(currentQuery);

  Object.entries(patch).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      delete nextQuery[key];
      return;
    }
    nextQuery[key] = String(value);
  });

  return nextQuery;
}

export function isSameRouteQuery(currentQuery: LocationQuery, nextQuery: LocationQueryRaw): boolean {
  const current = normalizeQueryRecord(currentQuery);
  const next = normalizeQueryRecord(nextQuery);
  const currentKeys = Object.keys(current).sort();
  const nextKeys = Object.keys(next).sort();

  if (currentKeys.length !== nextKeys.length) {
    return false;
  }

  return currentKeys.every((key, index) => key === nextKeys[index] && current[key] === next[key]);
}
