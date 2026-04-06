package com.example.music.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

//歌弹列表
@TableName(value = "song_list")
@Data
public class SongList {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;

    private String pic;

    private String style;

    private String introduction;

    private Integer type;

    //启用1、审核中2, 禁用3

    private int consumer;
    //用户ID

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(exist = false)
    private String reasonCode;

    @TableField(exist = false)
    private String reasonText;

    @TableField(exist = false)
    private Double recommendScore;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
