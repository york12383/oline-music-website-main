// 解析日期
export function getBirth(value) {
  if (value == null || value == "") return "";
  const date = new Date(value);
  const year = date.getFullYear();
  const month = date.getMonth() + 1 < 10 ? "0" + (date.getMonth() + 1) : date.getMonth() + 1;
  const day = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();
  return year + "-" + month + "-" + day;
}

/**
 * 表格时间格式化
 */
export function formatDate(cellValue) {
  if (cellValue == null || cellValue == "") return "";
  const date = new Date(cellValue);
  const year = date.getFullYear();
  const month = date.getMonth() + 1 < 10 ? "0" + (date.getMonth() + 1) : date.getMonth() + 1;
  const day = date.getDate() < 10 ? "0" + date.getDate() : date.getDate();
  const hours = date.getHours() < 10 ? "0" + date.getHours() : date.getHours();
  const minutes = date.getMinutes() < 10 ? "0" + date.getMinutes() : date.getMinutes();
  const seconds = date.getSeconds() < 10 ? "0" + date.getSeconds() : date.getSeconds();
  return year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
}

// 解析歌词
// export function parseLyric(text) {
//   let lines = text.split("\n");
//   const pattern = /\[\d{2}:\d{2}.(\d{3}|\d{2})\]/g;
//   const result = [];

//   // 对于歌词格式不对的特殊处理
//   if (!/\[.+\]/.test(text)) {
//     return [[0, text]];
//   }

//   while (!pattern.test(lines[0])) {
//     lines = lines.slice(1);
//   }

//   lines[lines.length - 1].length === 0 && lines.pop();
//   for (const item of lines) {
//     const time = item.match(pattern); // 存前面的时间段
//     const value = item.replace(pattern, ""); // 存歌词
//     for (const item1 of time) {
//       const t = item1.slice(1, -1).split(":");
//       if (value !== "") {
//         result.push([parseInt(t[0], 10) * 60 + parseFloat(t[1]), value]);
//       }
//     }
//   }
//   result.sort((a, b) => a[0] - b[0]);
//   return result;
// }

const extractLyricOffsetSeconds = (lrc: string): number => {
  const match = lrc.match(/\[offset:([+-]?\d+)\]/i);
  if (!match) {
    return 0;
  }

  const offsetMs = parseInt(match[1], 10);
  return Number.isFinite(offsetMs) ? offsetMs / 1000 : 0;
};

export const parseLyric = (lrc: string): [number, string][] => {
  if (!lrc || typeof lrc !== "string") {
    return [];
  }

  const offsetSeconds = extractLyricOffsetSeconds(lrc);
  const normalized = lrc
    .replace(/\r\n?/g, "\n")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/\[(?:ti|ar|al|by|offset):[^\]]*\]/gi, "\n");
  const chunkReg = /((?:\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?\])+)([^[]*)/g;
  const timeTagReg = /\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]/g;
  const result: [number, string][] = [];

  let chunkMatch: RegExpExecArray | null;
  while ((chunkMatch = chunkReg.exec(normalized)) !== null) {
    const timeGroup = chunkMatch[1] || "";
    const text = (chunkMatch[2] || "").replace(/\s+/g, " ").trim();
    if (!text) continue;

    timeTagReg.lastIndex = 0;
    let timeMatch: RegExpExecArray | null;
    while ((timeMatch = timeTagReg.exec(timeGroup)) !== null) {
      const minutes = parseInt(timeMatch[1], 10);
      const seconds = parseInt(timeMatch[2], 10);
      const milliseconds = parseInt((timeMatch[3] || "0").padEnd(3, "0").slice(0, 3), 10);
      const time = Math.max(0, minutes * 60 + seconds + milliseconds / 1000 + offsetSeconds);
      result.push([time, text]);
    }
  }

  result.sort((a, b) => a[0] - b[0]);
  return result;
};

// 解析播放时间
export function formatSeconds(value) {
  let theTime = parseInt(value);
  let theTime1 = 0;
  let theTime2 = 0;
  if (theTime > 60) {
    theTime1 = parseInt((theTime / 60).toString()); // 分
    theTime = parseInt((theTime % 60).toString()); // 秒
    // 是否超过一个小时
    if (theTime1 > 60) {
      theTime2 = parseInt((theTime1 / 60).toString()); // 小时
      theTime1 = 60; // 分
    }
  }
  // 多少秒
  let result = "";
  if (parseInt(theTime.toString()) < 10) {
    result = "0:0" + parseInt(theTime.toString());
  } else {
    result = "0:" + parseInt(theTime.toString());
  }
  // 多少分钟时
  if (theTime1 > 0) {
    if (parseInt(theTime.toString()) < 10) {
      result = "0" + parseInt(theTime.toString());
    } else {
      result = parseInt(theTime.toString()).toString();
    }
    result = parseInt(theTime1.toString()) + ":" + result;
  }
  // 多少小时时
  if (theTime2 > 0) {
    if (parseInt(theTime.toString()) < 10) {
      result = "0" + parseInt(theTime.toString());
    } else {
      result = parseInt(theTime.toString()).toString();
    }
    result = parseInt(theTime2.toString()) + ":" + parseInt(theTime1.toString()) + ":" + result;
  }
  return result;
}
