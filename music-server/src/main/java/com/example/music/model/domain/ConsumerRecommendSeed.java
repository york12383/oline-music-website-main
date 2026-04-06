package com.example.music.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("consumer_recommend_seed")
public class ConsumerRecommendSeed {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String seedType;

    private Integer refId;

    private String refValue;

    private Double weight;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
