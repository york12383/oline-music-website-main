package com.example.music.model.domain;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 歌曲评分实体类
 * 对应数据库表 `rank_song` (假设表名为 rank_song)
 */
@Data // 自动生成 Getter, Setter, toString, equals, hashCode
@Accessors(chain = true) // 开启链式设置方法，如 new RankSong().setId(1L).setScore(10);
@TableName("rank_song")
public class RankSong implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO) // 假设数据库ID是自增的
    private Long id;

    /**
     * 歌曲ID
     * 注意：如果数据库是 BIGINT，这里建议使用 Long
     */
    @TableField("song_id")
    private Integer songId;

    /**
     * 用户ID
     * 注意：如果数据库是 BIGINT，这里建议使用 Long
     */
    @TableField("consumer_id")
    private Integer consumerId;

    /**
     * 评分 (0-10分)
     */
    @TableField("score")
    private Integer score;
}