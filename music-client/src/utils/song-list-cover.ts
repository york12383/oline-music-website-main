import { HttpManager } from "@/api";

type SongListCoverSource = {
  id?: number | string;
  pic?: string;
};

const DEFAULT_SONG_LIST_COVER = "/img/songListPic/123.jpg";

const SONG_LIST_BACKEND_COVERS: Record<number, string> = {
  2: "/img/songListPic/109951162839104712.jpg",
  4: "/img/songListPic/109951162869937004.jpg",
  5: "/img/songListPic/109951162873752063.jpg",
  6: "/img/songListPic/109951163097151464.jpg",
  7: "/img/songListPic/109951163098238240.jpg",
  8: "/img/songListPic/109951163193554791.jpg",
  9: "/img/songListPic/109951163196627760.jpg",
  10: "/img/songListPic/109951163203287436.jpg",
  11: "/img/songListPic/109951163216834301.jpg",
  12: "/img/songListPic/109951163234476379.jpg",
  13: "/img/songListPic/109951163271025942.jpg",
  15: "/img/songListPic/109951163278666363.jpg",
  16: "/img/songListPic/109951163311246502.jpg",
  19: "/img/songListPic/109951163321304060.jpg",
  20: "/img/songListPic/109951163401615779.jpg",
  21: "/img/songListPic/109951163408189924.jpg",
  22: "/img/songListPic/109951163414509421.jpg",
  23: "/img/songListPic/109951163443093546.jpg",
  24: "/img/songListPic/109951163462173993.jpg",
  25: "/img/songListPic/109951163500933771.jpg",
  26: "/img/songListPic/109951163503924397.jpg",
  28: "/img/songListPic/109951163515798929.jpg",
  30: "/img/songListPic/109951163543366840.jpg",
  31: "/img/songListPic/109951163578540101.jpg",
  32: "/img/songListPic/109951163579465390.jpg",
  33: "/img/songListPic/109951163594594622.jpg",
  34: "/img/songListPic/109951163597665130.jpg",
  35: "/img/songListPic/109951163606909947.jpg",
  36: "/img/songListPic/109951163609743240.jpg",
  37: "/img/songListPic/109951163618525359.jpg",
  38: "/img/songListPic/109951163621168769.jpg",
  39: "/img/songListPic/109951163646671507.jpg",
  40: "/img/songListPic/109951163670223947.jpg",
  41: "/img/songListPic/109951163672593019.jpg",
  42: "/img/songListPic/109951163692248020.jpg",
  43: "/img/songListPic/109951163736178562.jpg",
  44: "/img/songListPic/109951163738160487.jpg",
  45: "/img/songListPic/109951163776201870.jpg",
  46: "/img/songListPic/109951163802235324.jpg",
  47: "/img/songListPic/109951163808060526.jpg",
  49: "/img/songListPic/109951163826485303.jpg",
  50: "/img/songListPic/109951163826685601.jpg",
  52: "/img/songListPic/109951163833244126.jpg",
  53: "/img/songListPic/109951163858422257.jpg",
  54: "/img/songListPic/109951163862683663.jpg",
  56: "/img/songListPic/109951163879964479.jpg",
  58: "/img/songListPic/109951163887710551.jpg",
  59: "/img/songListPic/109951163904955394.jpg",
  60: "/img/songListPic/109951163919069037.jpg",
  61: "/img/songListPic/109951163921593479.jpg",
  62: "/img/songListPic/109951163924312766.jpg",
  63: "/img/songListPic/109951163932838310.jpg",
  64: "/img/songListPic/109951163933917463.jpg",
  65: "/img/songListPic/109951163936991203.jpg",
  66: "/img/songListPic/109951163938242029.jpg",
  67: "/img/songListPic/109951163942747948.jpg",
  68: "/img/songListPic/1390882209600111343.jpg",
  69: "/img/songListPic/1390882209600343.jpg",
  70: "/img/songListPic/1390882211100783.jpg",
  71: "/img/songListPic/1560017681028a32415ca9a21f6f9a1d99b2731f224b5d319c424.jpg",
  72: "/img/songListPic/18577348464819001.jpg",
  73: "/img/songListPic/18591642116274551.jpg",
  74: "/img/songListPic/18612532836990988.jpg",
  75: "/img/songListPic/18731280092485571.jpg",
  77: "/img/songListPic/18804947371714354.jpg",
  78: "/img/songListPic/18814842976746273.jpg",
  79: "/img/songListPic/18878614648960788.jpg",
  81: "/img/songListPic/18907201951803673.jpg",
  82: "/img/songListPic/19053436998325469.jpg",
  83: "/img/songListPic/19079825277149145.jpg",
};

function normalizeId(value?: number | string) {
  const normalized = Number(value);
  return Number.isInteger(normalized) && normalized > 0 ? normalized : undefined;
}

function normalizePic(value?: string) {
  return String(value || "").trim();
}

function isSpecificSongListCover(pic: string) {
  return pic.startsWith("/img/songListPic/") && !pic.endsWith("/123.jpg") && !pic.endsWith("123.jpg");
}

function shouldPreferBackendSongListCover(pic: string) {
  if (!pic) {
    return true;
  }

  if (pic === DEFAULT_SONG_LIST_COVER || pic.endsWith("/123.jpg") || pic.endsWith("123.jpg")) {
    return true;
  }

  if (isSpecificSongListCover(pic)) {
    return false;
  }

  return (
    pic.startsWith("http") ||
    pic.startsWith("data:") ||
    pic.startsWith("/img/songPic/") ||
    pic.startsWith("/img/singerPic/")
  );
}

export function getSongListCoverPath(source: SongListCoverSource) {
  const pic = normalizePic(source.pic);
  const normalizedId = normalizeId(source.id);
  const backendCover = normalizedId ? SONG_LIST_BACKEND_COVERS[normalizedId] : "";

  if (!shouldPreferBackendSongListCover(pic)) {
    return pic || backendCover || DEFAULT_SONG_LIST_COVER;
  }

  return backendCover || pic || DEFAULT_SONG_LIST_COVER;
}

export function getSongListCoverUrl(source: SongListCoverSource) {
  return HttpManager.attachImageUrl(getSongListCoverPath(source));
}
