import { normalizeUserAvatarPath } from "@/constants/defaultAvatars";

export default {
  state: {
    userId: "", // ID
    username: "", // ??
    userPic: "", // ??
  },
  getters: {
    userId: (state) => state.userId,
    username: (state) => state.username,
    userPic: (state) => state.userPic,
  },
  mutations: {
    setUserId: (state, userId) => {
      state.userId = userId;
    },
    setUsername: (state, username) => {
      state.username = username;
    },
    setUserPic: (state, userPic) => {
      state.userPic = userPic ? normalizeUserAvatarPath(userPic) : "";
    },
    clearUserInfo: (state) => {
      state.userId = "";
      state.username = "";
      state.userPic = "";
    },
  },
};
