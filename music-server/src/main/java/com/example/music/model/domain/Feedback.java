package com.example.music.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String username;

    @TableField("feedback_type")
    private String feedbackType;

    private String title;

    private String content;

    private String contact;

    @TableField("page_path")
    private String pagePath;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
