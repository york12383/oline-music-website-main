package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.controller.UserSingerController;
import com.example.music.model.domain.Consumer;
import com.example.music.model.request.ConsumerRequest;
import com.example.music.model.request.SingerRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
//作用：服务接口，定义用户相关的业务逻辑方法。
public interface ConsumerService extends IService<Consumer> {

    R logout(HttpSession session);

    R addUser(ConsumerRequest registryRequest);

    R updateUserMsg(ConsumerRequest updateRequest);

    R updateUserAvator(MultipartFile avatorFile, int id, HttpSession session);

    R updatePassword(ConsumerRequest updatePasswordRequest);

    boolean existUser(String username);

    boolean verityPasswd(String username, String password);

    R deleteUser(Integer id, HttpSession session);

    R allUser(HttpSession session);

    R userOfId(Integer id);

    R loginStatus(ConsumerRequest loginRequest, HttpSession session);

    R loginEmailStatus(ConsumerRequest loginRequest, HttpSession session);

    R getSessionUser(HttpSession session);

    Consumer findByEmail (String email);

    R updatePassword01(ConsumerRequest updatePasswordRequest);


    R getUserSinger(Integer id);
    
    /**
     * 通过当前会话获取用户关联的歌手ID
     * @return 歌手ID
     */
    default R getUserSinger() {
        return getUserSinger(null);
    }

    R becomeSinger(UserSingerController.SingerRequests singerRequests, HttpSession session);

    R becomeSinger1(SingerRequest singerRequests, HttpSession session);


    R addPlayHistory(int songId, int userId, HttpSession session);

    R getRecentPlayHistory(int userId);

}
