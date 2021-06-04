package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.repository.entity.Good;
import com.example.repository.entity.Order;
import com.example.repository.mapper.GoodMapper;
import com.example.repository.mapper.OrderMapper;
import com.example.service.pojo.OrderInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 18:11:08
 */
@Service
public class OrderService {

    private GoodMapper goodMapper;

    private OrderMapper orderMapper;

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    // 查询订单的信息，在订单详情展示
    public OrderInfoDTO queryOrderInfo(String goodId, String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).ne("status", "2");
        Order order = orderMapper.selectOne(orderQueryWrapper);
        QueryWrapper<Good> goodQueryWrapper = new QueryWrapper<>();
        goodQueryWrapper.eq("good_id", goodId);
        Good good = goodMapper.selectOne(goodQueryWrapper);
        return new OrderInfoDTO(order, good.getGoodName(), good.getDiscountPrice());
    }
}
