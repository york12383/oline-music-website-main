import {deletes, get, getBaseURL, post} from './request'

function encodeRelativeResourceUrl(url: string) {
    if (!url) {
        return url;
    }

    const [pathWithQuery, hashFragment = ""] = url.split("#", 2);
    const [pathname, queryString = ""] = pathWithQuery.split("?", 2);
    const encodedPath = pathname
        .split("/")
        .map((segment, index) => {
            if (!segment) {
                return index === 0 ? "" : segment;
            }
            try {
                return encodeURIComponent(decodeURIComponent(segment));
            } catch (error) {
                void error;
                return encodeURIComponent(segment);
            }
        })
        .join("/");

    let rebuilt = encodedPath;
    if (queryString) {
        rebuilt += `?${queryString}`;
    }
    if (hashFragment) {
        rebuilt += `#${hashFragment}`;
    }
    return rebuilt;
}

const HttpManager = {

     logout: () => post(`/admin/logout`),

    // 获取图片信息，根据URL的前缀来决定如何处理，其实歌曲也是从这里获取的
    // attachImageUrl: (url) => {
    //     if (url.startsWith('http')) {
    //         return url;
    //     } else {
    //         return `${getBaseURL()}/${url}`;
    //     }
    // },
    // 获取图片信息，根据URL的前缀来决定如何处理，其实歌曲也是从这里获取的
    attachImageUrl: (url) => {
        if (!url) {
          return `${getBaseURL()}/img/songPic/tubiao.jpg`;
        }

        if (url.startsWith('http') || url.startsWith('data:')) {
          return url;
        }

        return `${getBaseURL()}${encodeRelativeResourceUrl(url)}`;
      },



    // =======================> 管理员 API 完成
    // 是否登录成功
    getLoginStatus: ({username, password}) => post(`admin/login/status`, {username, password}),
    getAdminSession: () => get('admin/session'),

    // =======================> 用户 API 完成
    // 返回所有用户
    getAllUser: () => get(`user`),
    // 返回指定ID的用户
    getUserOfId: (id) => get(`user/detail?id=${id}`),
    // 删除用户
    deleteUser: (id) => get(`user/delete?id=${id}`),
    // =======================> 收藏列表 API 完成
    // 返回的指定用户ID收藏列表
    getCollectionOfUser: (userId) => get(`collection/detail?userId=${userId}`),
    // 删除收藏的歌曲
    deleteCollection: (userId, songId) => deletes(`collection/delete?userId=${userId}&&songId=${songId}`),

    // =======================> 评论列表 API 完成
    // 获得指定歌曲ID的评论列表
    getCommentOfSongId: (songId) => get(`comment/song/detail?songId=${songId}`),
    // 获得指定歌单ID的评论列表
    getCommentOfSongListId: (songListId) => get(`comment/songList/detail?songListId=${songListId}`),
    // 删除评论
    deleteComment: (id) => deletes(`comment/delete?id=${id}`),

    // =======================> 歌手 API 完成
    // 返回所有歌手
    getAllSinger: () => get(`singer`),
    // 添加歌手
    setSinger: ({name, sex, birth, location, introduction}) => post(`singer/add`, {
        name,
        sex,
        birth,
        location,
        introduction
    }),
    // 更新歌手信息
    updateSingerMsg: ({id, name, sex, birth, location, introduction}) => post(`singer/update`, {
        id,
        name,
        sex,
        birth,
        location,
        introduction
    }),
    // 删除歌手
    deleteSinger: (id) => deletes(`singer/delete?id=${id}`),

    // =======================> 歌曲 API  完成
    // 返回所有歌曲
    getAllSong: () => get(`song`),
    // 分页获取歌曲
    getSongPages: ({ currentPage, pageSize, name, lyricStatus, resourceStatus }) => 
      get(`song/page?pageNum=${currentPage}&pageSize=${pageSize}&name=${name || ''}&lyricStatus=${lyricStatus || ''}&resourceStatus=${resourceStatus || ''}`),
    // 返回指定歌手ID的歌曲
    getSongOfSingerId: (id) => get(`song/singer/detail?singerId=${id}`),
    // 返回的指定用户ID收藏列表
    getSongOfId: (id) => get(`song/detail?id=${id}`),
    // 返回指定歌手名的歌曲
    getSongOfSingerName: (id) => get(`song/singerName/detail?name=${id}`),
    // 更新歌曲信息
    updateSongMsg: ({id, singerId, name, introduction, lyric,type}) => post(`song/update`, {
        id,
        singerId,
        name,
        introduction,
        lyric,
        type,
        
    }),
    updateSongUrl: (id) => `${getBaseURL()}/song/url/update?id=${id}`,
    updateSongImg: (id) => `${getBaseURL()}/song/img/update?id=${id}`,
    updateSongLrc: (id) => `${getBaseURL()}/song/lrc/update?id=${id}`,
    batchUpdateSongLrc: () => `${getBaseURL()}/song/lrc/batch/update`,
    // 删除歌曲
    deleteSong: (id) => deletes(`song/delete?id=${id}`),

    
    updateSongStatus:(songId,type) => post(`/song/admin/update?songId=${songId}&type=${type}`),

    // =======================> 歌单 API 完成
    // 添加歌单t
    setSongList: ({title, introduction, style}) => post(`/songList/add`, {title, introduction, style}),
    // 获取全部歌单
    getSongList: () => get(`songList`),
    // 更新歌单信息
    updateSongListMsg: ({id, title, introduction, style,type,consumer}) => post(`songList/update`, {id, title, introduction, style,type,consumer}),
    // 删除歌单
    deleteSongList: (id) => get(`songList/delete?id=${id}`),

    // =======================> 歌单歌曲 API 完成
    // 给歌单添加歌曲
    setListSong: ({songId,songListId}) => post(`listSong/user/add`, {songId,songListId}),
    // 返回歌单里指定歌单ID的歌曲
    getListSongOfSongId: (songListId) => get(`listSong/detail?songListId=${songListId}`),
    // 删除歌单里的歌曲
    deleteListSong: (songId) => get(`listSong/delete?songId=${songId}`),
    
    // 获取所有轮播图
    getAllbanner: () => get(`banner/getAllBanner`),
    // 删除轮播图
    deleteBanner: (id) => deletes(`banner/deleteBannerOfId?id=${id}`),
    // 添加轮播图
    addBanner: (linkUrl) => post(`banner/addBanner`, { linkUrl }),
    // 上传轮播图
    handleImageUploadSuccess:() => post(`banner/uploadImg`), 


    //增加管理员
    addAdmin: ({id, username, password}) => post(`admin/add`, {id, username, password}),
    
    //获取管理员
    getAdmin: () => get(`admin/administrator`),

    // 删除管理员
    deleteAdmin:(id) => deletes(`admin/delete?id=${id}`),


    // 在 HttpManager 对象中添加以下方法
    getAllRankByType: ({type, currentPage, pageSize, name, consumerName}) => 
        get(`/rankList/allByType?type=${type}&currentPage=${currentPage}&pageSize=${pageSize}&name=${name}&consumerName=${consumerName}`),


    deleteRank: (type,id) => deletes(`/rankList/delete?type=${type}&id=${id}`),

    // 反馈管理
    getAllFeedback: () => get(`feedback/admin/all`),
    updateFeedbackStatus: (id, status) => post(`feedback/admin/status?id=${id}&status=${status}`),
    deleteFeedback: (id) => deletes(`feedback/admin/delete?id=${id}`),



    // 首页数据
    getHomeCount:() => get(`info/home/count`),

    // 审核总览
    getAuditOverview:() => get(`info/audit/overview`),

    // 资源健康度
    getContentHealth:() => get(`info/content/health`),

    // 热门歌曲监控
    getTop10Songs:() => get(`info/song/top10`),

    // 用户活跃趋势
    getRecentUserActivity:(range = "30d") => get(`info/user/activity?range=${range}`),

    // 新增内容趋势
    getNewContentTrend:(range = "7d") => get(`info/content/trend?range=${range}`),

    // 用户年龄分布
    getUserAgeDistribution:()=> get(`info/user/age/distribution`),

    // 用户性别分布
    getUserSexDistribution:()=> get(`info/user/sex/distribution`),

    // 反馈趋势
    getRecentFeedbackTrend: (range = "7d") => get(`info/feedback/trend?range=${range}`),

    

} 

export {HttpManager}






