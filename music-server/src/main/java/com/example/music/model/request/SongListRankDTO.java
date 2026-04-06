package com.example.music.model.request;

import lombok.Data;

import java.util.Date;

@Data
public class SongListRankDTO {
    private Long id;
    private String songListName;  // 歌单名
    private String consumerName;  // 用户昵称
    private Integer score;        // 评分
    private Date createTime;      // 评分时间
    // 其他需要的字段...
}
