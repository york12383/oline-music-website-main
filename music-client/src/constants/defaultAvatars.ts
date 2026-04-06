export const LEGACY_DEFAULT_USER_AVATAR = "/img/avatorImages/user.jpg";
export const DEFAULT_USER_AVATAR_COUNT = 40;
export const DEFAULT_USER_AVATAR_PREFIX = "/img/avatorImages/defaults/default-avatar-";

export const buildDefaultUserAvatarPath = (index: number) =>
  `${DEFAULT_USER_AVATAR_PREFIX}${String(index).padStart(2, "0")}.jpg`;

export const DEFAULT_USER_AVATAR_OPTIONS = Array.from(
  { length: DEFAULT_USER_AVATAR_COUNT },
  (_, index) => ({
    id: index + 1,
    label: `默认头像 ${String(index + 1).padStart(2, "0")}`,
    path: buildDefaultUserAvatarPath(index + 1),
  })
);

export const normalizeUserAvatarPath = (avatar?: string | null) => {
  if (!avatar || avatar === LEGACY_DEFAULT_USER_AVATAR) {
    return DEFAULT_USER_AVATAR_OPTIONS[0].path;
  }
  return avatar;
};
