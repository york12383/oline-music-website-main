package com.example.music.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

@TableName(value = "comment")
@Data
public class Comment {
    @TableId(type = IdType.AUTO)
    private Integer id; //主键

    private Integer userId; //用户

    private Integer songId; //歌曲

    private Integer songListId;  //歌单

    private String content; //评论内容

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Byte type; //1:歌曲 2:歌单

    private Integer up; //点赞

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
