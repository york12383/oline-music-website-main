package com.example.music.model.request;

import lombok.Data;

import java.util.Date;

/**
 * @Author 祝英台炸油条
 * @Time : 2022/6/5 19:35
 * 这块 现在尝试把所有有关用户的属性都放入
 **/

//    作用：请求数据封装类，用于接收客户端传来的用户数据。
//    使用 Lombok：通过 @Data 自动生成 getter、setter、toString 等方法。
//    字段：包含用户的所有属性，如 username、password、email 等

@Data
public class ConsumerRequest {
    private Integer id;

    private String username;

    private String oldPassword; //因为会用到用户旧密码 这无所谓的对应即可

    private String password;

    private Byte sex;

    private String phoneNum;

    private String email;

    private Date birth;

    private String introduction;

    private String location;

    private String avator;

    private Date createTime;

    private Integer singerId;


}
