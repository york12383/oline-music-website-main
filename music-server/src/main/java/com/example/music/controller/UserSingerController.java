package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.SingerRequest;
import com.example.music.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
public class UserSingerController {

    @Autowired
    private ConsumerService consumerService;

    /**
     * 获取用户对应的歌手信息
     */
    @GetMapping("/user/singer/detail")
    public R getUserSinger(@RequestParam Integer id) {
        return consumerService.getUserSinger(id);
    }

    /**
     * 用户成为歌手
     */
    @PostMapping("/user/singer/add")
    public R becomeSinger(@RequestBody SingerRequests singerRequests, HttpSession session) {
        //System.out.println("UserSingerController.java传入的值" + singerRequests.birth +"\n" + singerRequests.location +"\n" + singerRequests.introduction +"\n" + singerRequests.name);
        //return null;
        return consumerService.becomeSinger(singerRequests, session);
    }


    //测试
    @PostMapping("/user/singer/add1")
    public R becomeSinger1(@RequestBody SingerRequest singerRequest, HttpSession session) {
        return consumerService.becomeSinger1(singerRequest, session);
    }

    // 创建一个请求数据传输对象
    public static class SingerRequests {
        private String name;
        private Integer sex;
        private String birth;
        private String location;
        private String introduction;
        private Integer userId;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSex() {
            return sex;
        }

        public void setSex(Integer sex) {
            this.sex = sex;
        }

        public String getBirth() {
            return birth;
        }

        public void setBirth(String birth) {
            this.birth = birth;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getIntroduction() {
            return introduction;
        }

        public void setIntroduction(String introduction) {
            this.introduction = introduction;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }
    }

}
