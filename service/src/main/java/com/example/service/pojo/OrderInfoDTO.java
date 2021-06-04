package com.example.service.pojo;

import com.example.repository.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 20:12:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrderInfoDTO {
    private Order order;
    private String goodName;
    private BigDecimal deal;
}
