import { getBaseURL, get, post, postJson, deletes } from "./request";

const HttpManager = {

  logout(): void {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("userName");
    localStorage.removeItem("userAvatar");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userPhoneNum");
    localStorage.removeItem("userSex");
    localStorage.removeItem("userBirth");
    localStorage.removeItem("userIntroduction");
    
    post('logout')
   },

  // 获取图片信息
  //attachImageUrl: (url) => url ? `${getBaseURL()}/${url}` : "https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png",
    // 获取图片信息，根据URL的前缀来决定如何处理，其实歌曲也是从这里获取的
    attachImageUrl: (url) => {
      if (!url) {
        return `${getBaseURL()}/img/songPic/tubiao.jpg`;
      }

      if (url.startsWith('http') || url.startsWith('data:')) {
        return url;
      }

      return `${getBaseURL()}${url}`;
    },

    getDefaultImageUrl: () => `${getBaseURL()}/img/songPic/tubiao.jpg`,

  // =======================> 用户 API 完成
  // 登录
  signIn: ({username,password}) => post(`user/login/status`, {username,password}),
  signInByemail: ({email,password})=>post(`user/email/status`, {email,password}),
  getSessionUser: () => get(`user/session`),
  sendVerificationCode: ({ email }) => get(`user/sendVerificationCode?email=${encodeURIComponent(email)}`),
  resetPassword: ({ email, code, password, confirmPassword }) =>
    post(`user/resetPassword`, { email, code, password, confirmPassword }),
  // 注册
  SignUp: ({username,password,sex,phoneNum,email,birth,introduction,location}) => post(`user/add`, {username,password,sex,phoneNum,email,birth,introduction,location}),
  // 删除用户
  deleteUser: (id) => get(`user/delete?id=${id}`),
  // 更新用户信息
  updateUserMsg: (payload: {
    id: any;
    username?: any;
    sex?: any;
    phoneNum?: any;
    email?: any;
    birth?: any;
    introduction?: any;
    location?: any;
    avator?: any;
  }) => post(`user/update`, payload),
  updateUserPassword: ({id,username,oldPassword,password}) => post(`user/updatePassword`, {id,username,oldPassword,password}),
  // 返回指定ID的用户
  getUserOfId: (id) => get(`user/detail?id=${id}`),
  // 更新用户头像
  uploadUrl: (userId) => `${getBaseURL()}/user/avatar/update?id=${userId}`,

  // =======================> 歌单 API 完成
  // 获取全部歌单
  getSongList: () => get("songList"),

  // 获取最热歌单
  getHotSongLists: (limit = 10) => get(`/songList/hot?limit=${limit}`),
  // 获取热门歌曲
  getHotSongs: (limit = 10) => get(`/song/hot?limit=${limit}`),
  // 获取全部歌曲
  getAllSongs: () => get("song"),

  // 获取指定歌单
  getSongListOfId: (id) => get(`songList/detail?id=${id}`),

  // 获取歌单类型
  getSongListOfStyle: (style) => get(`songList/style/detail?style=${style}`),
  // 返回标题包含文字的歌单
  getSongListOfLikeTitle: (keywords) => get(`songList/likeTitle/detail?title=${keywords}`),
  // 返回歌单里指定歌单ID的歌曲
  getListSongOfSongId: (songListId) => get(`listSong/detail?songListId=${songListId}`),

  // 获取歌单分页查询
  getsongListByPage:(currentPage) => get(`songList/page?currentPage=${currentPage}`),

  //获取用户的歌单
  getSongListByConsumerId:(id) => get(`songList/user/detail?id=${id}`),
  
  //删除歌单里的歌曲
  deleteListSongOfId: (id,currentSongListId) => get(`/listSong/user/delete?id=${id}&listSongId=${currentSongListId}`),

  //删除歌单
  deleteSongList:(id) => get(`/songList/delete?id=${id}`),

  // 添加歌单
  addSongList: ({title,introduction,style}) => post(`songList/user/add`,{
    title,
    introduction,
    style,
  }),

  updateSongList:({id,title,introduction,style,pic}) => post(`songList/user/update`,{
    id,
    title,
    introduction,
    style,
    pic
  }),

  updateSongListPic: (formData) => post(`songList/img/update`, formData),

  setListSong: ({songId, songListId}) => post(`listSong/add`, {songId, songListId}),

  // =======================> 歌手 API  完成
  // 返回所有歌手
  getAllSinger: () => get("singer"),
  // 通过性别对歌手分类
  getSingerOfSex: (sex) => get(`singer/sex/detail?sex=${sex}`),

    // 获取用户歌手id
    getUserSinger: (id) => get(`user/singer/detail?id=${id}`),

    //成为歌手并返回对应歌手id
    becomeSinger: ({name, sex, birth, location, introduction,userId}) => post(`user/singer/add`,{
      name,
      sex,
      birth,
      location,
      introduction,
      userId
  }),
    // 删除歌曲
    deleteSong: (id) => deletes(`song/delete?id=${id}`),
    // 添加歌手      原版
    setSinger: ({name, sex, birth, location, introduction}) => post(`singer/add`, {
        name,
        sex,
        birth,
        location,
        introduction
    }),
    // 更新歌手信息   原版
    updateSingerMsg: ({id, name, sex, birth, location, introduction}) => post(`singer/update`, {
        id,
        name,
        sex,
        birth,
        location,
        introduction
    }),
    // 删除歌手      原版
    deleteSinger: (id) => deletes(`singer/delete?id=${id}`),

  getHotSingers: (limit = 10) => get(`/singer/hot?limit=${limit}`),
//========================================================================================================================================================================================


  // =======================> 收藏 API 完成
  // 返回的指定用户ID的收藏列表
  getCollectionOfUser: (userId, type?) => get(type === undefined ? `collection/detail?userId=${userId}` : `collection/detail?userId=${userId}&type=${type}`),
  // 添加收藏 type: 0 代表歌曲， 1 代表歌单
  setCollection: ({userId,type,songId,songListId}: {userId: any; type: any; songId?: any; songListId?: any}) => post(`collection/add`,{userId,type,songId,songListId}),

  deleteCollection: ({userId, type, songId, songListId}: {userId: any; type?: any; songId?: any; songListId?: any}) => {
    const query = new URLSearchParams();
    query.append("userId", String(userId));
    if (type !== undefined && type !== null) query.append("type", String(type));
    if (songId !== undefined && songId !== null) query.append("songId", String(songId));
    if (songListId !== undefined && songListId !== null) query.append("songListId", String(songListId));
    return deletes(`collection/delete?${query.toString()}`);
  },

  isCollection: ({userId, type, songId, songListId}: {userId: any; type?: any; songId?: any; songListId?: any}) => post(`collection/status`, {userId, type, songId, songListId}),

  // =======================> 评分 API 完成
  // 提交评分
  setRank: ({songListId,consumerId,score}) => post(`rankList/add`, {songListId,consumerId,score}),
  // 获取指定歌单的评分
  getRankOfSongListId: (songListId) => get(`rankList?songListId=${songListId}`),
  // 获取指定用户的歌单评分
  getUserRank: (consumerId, songListId) => get(`/rankList/user?consumerId=${consumerId}&songListId=${songListId}`),
  // 提交指定歌曲的评分
  setSongRank: ({songId,consumerId,score}) => post(`rankList/addsong`, {songId,consumerId,score}),
  // 获取指定歌曲的评分
  getRankOfSongId: (songId) => get(`rankList/songRank?songId=${songId}`),
  // 获取指定用户的歌曲评分
  getUserRankSong: ({consumerId, songId}) => get(`/rankList/userRankSong?consumerId=${consumerId}&songId=${songId}`),
  // 删除指定歌曲的评分
  deleteSongRank:({songId, consumerId}) => deletes(`rankSong/deleted?songId=${songId}&consumerId=${consumerId}`),
  // 删除指定歌单的评分
  deleteRankList: ({songListId,consumerId}) => deletes(`rankList/deletedd?songListId=${songListId}&consumerId=${consumerId}`),

  // =======================> 评论 API 完成
  // 添加评论
  setComment: ({userId,content,songId,songListId,nowType}) => post(`comment/add`, {userId,content,songId,songListId,nowType}),
  // 删除评论
  deleteComment: (id) => get(`comment/delete?id=${id}`),
  // 点赞
  setSupport: ({id,up}) => post(`comment/like`, {id,up}),
  // 返回所有评论
  getAllComment: (type, id) => {
    let url = "";
    if (type === 1) {
      url = `comment/songList/detail?songListId=${id}`;
    } else if (type === 0) {
      url = `comment/song/detail?songId=${id}`;
    }
    return get(url);
  },

  // =======================> 歌曲 API


  // 获取用户的歌曲
  songOfuserSingerId: () => get(`/song/singer/user/detail`),

  // 返回指定歌曲ID的歌曲
  getSongOfId: (id) => get(`song/detail?id=${id}`),
  // 返回指定歌手ID的歌曲
  getSongOfSingerId: (id) => get(`song/singer/detail?singerId=${id}`),
  getSongOfSingerIdByPage: (id, pageNum = 1, pageSize = 10) =>
    get(`song/singer/page?singerId=${id}&pageNum=${pageNum}&pageSize=${pageSize}`),
  // 返回指定歌手名的歌曲
  getSongOfSingerName: (keywords) => get(`song/singerName/detail?name=${keywords}`),
  
  // 下载音乐，根据URL的前缀来决定如何处理
  downloadMusic: (url) => {
    if (url.startsWith('https')) {
      // 对于以http开头的完整URL，直接下载
      return get(url, { responseType: "blob" });
    } else {
      // 对于相对路径，添加基础URL前缀
      const fullUrl = `${getBaseURL()}/${url}`;
      return get(fullUrl, { responseType: "blob" });
    }
  },

  updateSongMsg:({id,singerId,name,introduction,lyric}) => post('/song/update', {id,singerId,name,introduction,lyric}),

  // 获取用户最近播放记录
  getRecentPlayHistory: (userId) => get(`playHistory/user?userId=${userId}`),

  // 添加歌曲播放记录
  addPlayHistory:({songId,userId}) => post(`playHistory/add?songId=${songId}&userId=${userId}`),

  // 更新歌曲状态
  updateSongStatus:({songId,singerId}) => post(`song/update/status?songId=${songId}&singerId=${singerId}`),

  //======================> 点赞api的优化 避免有些是重复的点赞！新增数据表了得

  testAlreadySupport:({commentId,userId}) => post(`userSupport/test`, {commentId,userId}),

  deleteUserSupport:({commentId,userId}) => post(`userSupport/delete`, {commentId,userId}),

  insertUserSupport:({commentId,userId}) => post(`userSupport/insert`, {commentId,userId}),

  //获取所有的海报，轮播图
  getBannerList: () => get("banner/getAllBanner"),
  
  // 获取推荐歌曲
  getRecommendSongList: (limit = 10) => get(`recommend/data?limit=${limit}`),
  // 获取推荐歌单
  getRecommendSongLists: (limit = 10) => get(`recommend/song-lists?limit=${limit}`),
  // 获取推荐歌手
  getRecommendSingers: (limit = 10) => get(`recommend/singers?limit=${limit}`),
  // 冷启动偏好选项
  getRecommendBootstrapOptions: () => get(`recommend/bootstrap/options`),
  // 保存冷启动偏好
  saveRecommendBootstrapPreferences: ({ likedSingerIds = [], likedSongIds = [], likedStyles = [] }) =>
    post(`recommend/bootstrap/preferences`, { likedSingerIds, likedSongIds, likedStyles }),
  // 推荐调试画像
  getRecommendDebugProfile: () => get(`recommend/debug/profile`),

  // =======================> 站内信息与反馈
  addFeedback: ({ feedbackType, title, content, contact, pagePath }) =>
    post(`feedback/add`, { feedbackType, title, content, contact, pagePath }),
  getMyFeedback: () => get(`feedback/my`),

  assistantChat: ({ message, history = [], allowImport = true }) =>
    postJson(`assistant/chat`, { message, history, allowImport }, { timeout: 180000 }),
  
 

};



export { HttpManager };


