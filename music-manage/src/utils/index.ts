// 解析日期
export function getBirth(cellValue) {
  if (cellValue == null || cellValue == "") return "";
  const date = new Date(cellValue);
  const year = date.getFullYear();
  const month = date.getMonth() + 1 < 10 ? "0" + (date.getMonth() + 1) : date.getMonth() + 1;
  const day = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();
  return year + "-" + month + "-" + day;
}

// 解析歌词
export function parseLyric(text) {
  const lines = text.split("\n");
  const pattern = /\[\d{2}:\d{2}(?::\d{2}|[.:](\d{3}|\d{2}))?\]/g;
  const result = [];

  // 对于歌词格式不对的特殊处理
  if (!/\[.+\]/.test(text)) {
    return [text];
  }
  for (const item of lines) {
    if (pattern.test(item)) {
      const value = item.replace(pattern, ""); // 存歌词
      result.push(value);
    }
  }
  return result;
}

export type LyricStatusKey = "missing" | "placeholder" | "abnormal" | "ready";

export function normalizeLyricText(text?: string | null) {
  return (text || "").trim();
}

export function isInstrumentalLyric(text?: string | null) {
  const normalized = normalizeLyricText(text);
  return /纯音乐|轻音乐|钢琴曲|请欣赏/i.test(normalized);
}

export function isPlaceholderLyric(text?: string | null) {
  const normalized = normalizeLyricText(text);
  return normalized.includes("暂无歌词") && !isInstrumentalLyric(normalized);
}

export function isTimedLyric(text?: string | null) {
  const normalized = normalizeLyricText(text);
  return /\[\d{2}:\d{2}(?::\d{2}|(?:\.\d{2,3}))?\]/.test(normalized);
}

export function getLyricStatus(text?: string | null): LyricStatusKey {
  const normalized = normalizeLyricText(text);

  if (!normalized) {
    return "missing";
  }

  if (isPlaceholderLyric(normalized)) {
    return "placeholder";
  }

  if (isInstrumentalLyric(normalized)) {
    return "ready";
  }

  if (!isTimedLyric(normalized)) {
    return "abnormal";
  }

  return "ready";
}

export function getLyricStatusLabel(status: LyricStatusKey) {
  switch (status) {
    case "missing":
      return "未填写";
    case "placeholder":
      return "占位歌词";
    case "abnormal":
      return "格式异常";
    case "ready":
      return "已收录";
    default:
      return "未知";
  }
}

export function getLyricStatusTagType(status: LyricStatusKey) {
  switch (status) {
    case "missing":
      return "info";
    case "placeholder":
      return "warning";
    case "abnormal":
      return "danger";
    case "ready":
      return "success";
    default:
      return "info";
  }
}
