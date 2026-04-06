package com.example.music.model.request;

import lombok.Data;

import java.util.Date;

// 歌曲评分返回DTO
@Data
public class SongRankDTO {
    private Long id;
    private String songName;      // 歌曲名
    private String consumerName;  // 用户昵称
    private Integer score;        // 评分
    private Date createTime;      // 评分时间
    // 其他需要的字段...
}