package com.example.music.model.request;

import lombok.Data;

import java.util.Date;

/**
 * @Author 祝英台炸油条
 * @Time : 2022/6/6 13:11
 **/
@Data
public class CollectRequest {
    private Integer id;

    private Integer userId;

    //0:歌曲 1:歌单
    private Byte type;

    private Integer songId;

    private Integer songListId;

    private Date createTime;
}
