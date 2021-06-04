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
import java.time.LocalDateTime;

/**
 * @author ice
 * @create 2021-05-26 22:00:52
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sec_orders")
public class Order implements Serializable {
    private static final long serialVersionUID = -1594951159465127479L;
    @TableId(type = IdType.ASSIGN_UUID)
    private String orderId;

    private String phone;
    private String email;
    private String goodId;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createTime;
    private String status;
}
