package com.example.repository.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author ice
 * @create 2021-05-26 22:00:44
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sec_goods")
public class Good implements Serializable {
    private static final long serialVersionUID = -8595790565352087597L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String goodId;
    private String goodName;
    private BigDecimal originPrice;
    private BigDecimal discountPrice;
    private Long stock;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime  startTime;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime endTime;
}
