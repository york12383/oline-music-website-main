package com.example.music.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("play_history")
public class PlayHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private int userId;

    private int songId;

    // 播放时间
    private Date playTime;

    // (可选) 实际播放了多少秒，用于判断是否是有效播放
    // 比如：只有听超过30秒才算一次有效记录，防止刷数据
    private Integer duration;
}

//CREATE TABLE `play_history` (
//        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
//        `user_id` bigint(20) NOT NULL COMMENT '用户ID',
//        `song_id` bigint(20) NOT NULL COMMENT '歌曲ID',
//        `play_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
//        `duration` int(11) DEFAULT 0 COMMENT '实际收听时长(秒)，可选',
//PRIMARY KEY (`id`),
//KEY `idx_user_time` (`user_id`, `play_time`) USING BTREE COMMENT '用于查询某人的最近播放',
//KEY `idx_song` (`song_id`) USING BTREE COMMENT '用于统计歌曲热度'
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户播放记录表';