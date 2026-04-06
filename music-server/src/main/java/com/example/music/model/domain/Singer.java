package com.example.music.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

@TableName(value = "singer")
@Data
public class Singer {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Byte sex;

    private String pic;

    private Date birth;

    private String location;

    private String introduction;

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
