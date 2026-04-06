package com.example.music.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public final class DefaultAvatarUtils {
    public static final int DEFAULT_AVATAR_COUNT = 40;
    public static final String LEGACY_DEFAULT_AVATAR = "/img/avatorImages/user.jpg";
    private static final String DEFAULT_AVATAR_PREFIX = "/img/avatorImages/defaults/default-avatar-";

    private DefaultAvatarUtils() {
    }

    public static String buildDefaultAvatarPath(int index) {
        int safeIndex = Math.floorMod(index - 1, DEFAULT_AVATAR_COUNT) + 1;
        return DEFAULT_AVATAR_PREFIX + String.format("%02d", safeIndex) + ".jpg";
    }

    public static String resolvePreparedDefaultAvatar(Integer consumerId, String username) {
        int seed = consumerId != null ? consumerId : Objects.hashCode(StringUtils.defaultString(username));
        return buildDefaultAvatarPath(Math.floorMod(seed, DEFAULT_AVATAR_COUNT) + 1);
    }

    public static boolean shouldUsePreparedDefault(String avatar) {
        return StringUtils.isBlank(avatar) || Objects.equals(avatar, LEGACY_DEFAULT_AVATAR);
    }

    public static String normalizeAvatar(String avatar, Integer consumerId, String username) {
        if (shouldUsePreparedDefault(avatar)) {
            return resolvePreparedDefaultAvatar(consumerId, username);
        }
        return avatar;
    }
}
