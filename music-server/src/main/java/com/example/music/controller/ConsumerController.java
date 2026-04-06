package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.domain.Consumer;
import com.example.music.model.domain.ResetPasswordRequest;
import com.example.music.model.request.ConsumerRequest;
import com.example.music.service.ConsumerService;
import com.example.music.service.impl.ConsumerServiceImpl;
import com.example.music.service.impl.SimpleOrderManager;
import com.example.music.utils.RandomUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

//作用：控制器层，负责接收 HTTP 请求，调用业务逻辑层（Service），并返回响应

@RestController//@RestController：表示这是一个 RESTful 控制器
public class ConsumerController {

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    ConsumerServiceImpl consumerServiceimpl;

    @Autowired
    private SimpleOrderManager simpleOrderManager;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    /**
     * TODO 前台页面调用 注册
     * 用户注册
     */
    //@PostMapping / @GetMapping：分别用于处理 POST 和 GET 请求
    @PostMapping("/user/add")

    public R addUser(@RequestBody ConsumerRequest registryRequest) {
                //通过@RequestBody 将请求体反序列化为 ConsumerRequest 对象；
        return consumerService.addUser(registryRequest);
        //调用 consumerService.addUser() 方法完成用户注册逻辑
    }//返回统一响应类 R 给前端，包含注册结果信息

    /*
    用户注册流程（以 addUser 为例）
    前端发送请求：POST /user/add，携带 ConsumerRequest 数据。
    ConsumerController.java：
        接收到请求后，调用 consumerService.addUser(registryRequest)。
    ConsumerServiceImpl.java（未展示）：
        实现 addUser 方法，调用 ConsumerMapper 进行数据库操作。
    ConsumerMapper.java：
        使用 MyBatis-Plus 的 insert 方法将数据插入数据库。
    返回结果：最终通过 R（通用响应类）返回给前端。
            2.2 用户登录流程（以 loginStatus 为例）
    前端发送请求：POST /user/login/status，携带 ConsumerRequest 数据。
    ConsumerController.java：
    接收到请求后，调用 consumerService.loginStatus(loginRequest, session)。
    ConsumerServiceImpl.java：
    实现 loginStatus 方法，验证用户名和密码是否正确。
    如果正确，将用户信息存入 HttpSession。
    返回结果：返回登录状态给前端。
     */
    /**
     * TODO 前台页面调用  登录
     * 登录判断
     */
    @PostMapping("/user/login/status")
    public R loginStatus(@RequestBody ConsumerRequest loginRequest, HttpSession session) {
        return consumerService.loginStatus(loginRequest, session);
    }
    /**
     * email登录
     */
    @PostMapping("/user/email/status")
    public R loginEmailStatus(@RequestBody ConsumerRequest loginRequest, HttpSession session) {
        return consumerService.loginEmailStatus(loginRequest, session);
    }

    @GetMapping("/user/session")
    public R getSessionUser(HttpSession session) {
        return consumerService.getSessionUser(session);
    }

    /**
     * TODO 前台页面调用  登出
     * 登出
     */
    @PostMapping("logout")
    public R logout( HttpSession session) {
        return consumerService.logout(session);
    }

    /**
     * 密码恢复（忘记密码）
     */
    @PostMapping("/user/resetPassword")
    public R resetPassword(@RequestBody ResetPasswordRequest passwordRequest){
        Consumer user = consumerService.findByEmail(passwordRequest.getEmail());
        String code = stringRedisTemplate.opsForValue().get("code");
        if (user==null){
            return R.fatal("用户不存在");
        }else if (!code.equals(passwordRequest.getCode())){
            return R.fatal("验证码不存在或失效");
        }
        ConsumerRequest consumerRequest=new ConsumerRequest();
        BeanUtils.copyProperties(user, consumerRequest);
        System.out.println(user);
        System.out.println(consumerRequest);
        consumerRequest.setPassword(passwordRequest.getPassword());
        consumerServiceimpl.updatePassword01(consumerRequest);
        return R.success("密码修改成功");
    }

    /**
     * 发送验证码功能
     */
    @GetMapping("/user/sendVerificationCode")
    public R sendCode(@RequestParam String email){
        Consumer user = consumerService.findByEmail(email);
        if (user==null){
            return R.fatal("用户不存在");
        }
        String code = RandomUtils.code();
        simpleOrderManager.sendCode(code,email);
        //保存在redis中
        stringRedisTemplate.opsForValue().set("code",code,5, TimeUnit.MINUTES);
        return R.success("发送成功");
    }

    /**
     * TODO 管理界面调用
     * 返回所有用户
     */
    @GetMapping("/user")
    public R allUser(HttpSession session) {
        return consumerService.allUser(session);
    }


    /**
     * TODO 用户界面调用
     * 返回指定 ID 的用户
     */
    @GetMapping("/user/detail")
    public R userOfId(@RequestParam int id) {
        return consumerService.userOfId(id);
    }

    /**
     * TODO 管理界面的调用
     * 删除用户
     */
    @GetMapping("/user/delete")
    public R deleteUser(@RequestParam int id, HttpSession session) {
        return consumerService.deleteUser(id, session);
    }

    /**
     * TODO 前后台界面的调用
     * 更新用户信息
     */
    @PostMapping("/user/update")
    public R updateUserMsg(@RequestBody ConsumerRequest updateRequest) {
        return consumerService.updateUserMsg(updateRequest);
    }

    /**
     * TODO 前后台更新用户的密码
     * 更新用户密码
     */
    @PostMapping("/user/updatePassword")
    public R updatePassword(@RequestBody ConsumerRequest updatePasswordRequest) {
        return consumerService.updatePassword(updatePasswordRequest);
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/user/avatar/update")
    public R updateUserPic(@RequestParam("file") MultipartFile avatorFile, @RequestParam("id") int id,HttpSession session) {
        return consumerService.updateUserAvator(avatorFile, id, session);
    }
    //播放记录
    @PostMapping("/playHistory/add")
    public R addPlayHistory(@RequestParam int songId, @RequestParam int userId,HttpSession session) {
        return consumerService.addPlayHistory(songId, userId,session);
    }

    //获取用户最近播放记录
    @GetMapping("/playHistory/user")
    public R getRecentPlayHistory(@RequestParam int userId) {
        return consumerService.getRecentPlayHistory(userId);
    }

}
