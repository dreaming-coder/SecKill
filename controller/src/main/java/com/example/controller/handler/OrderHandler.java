package com.example.controller.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.repository.entity.Order;
import com.example.repository.mapper.OrderMapper;
import com.example.service.pojo.OrderInfoDTO;
import com.example.service.service.OrderService;
import com.example.service.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-03 21:36:01
 */
@Controller
public class OrderHandler {

    private OrderMapper orderMapper;

    private OrderService orderService;

    private PayService payService;

    @Autowired
    public void setOrderMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Autowired
    public void setPayService(PayService payService) {
        this.payService = payService;
    }

    @ResponseBody
    @GetMapping("/exist/{goodId}/{phone}")
    public Boolean exists(@PathVariable String goodId, @PathVariable String phone) {
        QueryWrapper<Order> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("phone", phone).eq("good_id", goodId).ne("status", 2);
        Order order = orderMapper.selectOne(orderQueryWrapper);
        return order != null;
    }

    @ResponseBody
    @PostMapping("/pay")
    public Boolean pay(@RequestParam("goodId") String goodId, @RequestParam("phone") String phone) {
        try {
            payService.pay(goodId, phone);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @ResponseBody
    @PostMapping("/cancel")
    public Boolean cancel(@RequestParam("goodId") String goodId, @RequestParam("phone") String phone) {
        try {
            payService.cancel(goodId, phone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @ResponseBody
    @GetMapping("/later/{phone}")
    public Boolean later(@PathVariable String phone) {
        return true;
    }

    @RequestMapping(value = "/order")
    public ModelAndView order(@RequestParam("goodId") String goodId, @RequestParam("phone") String phone) {
        OrderInfoDTO orderInfo = orderService.queryOrderInfo(goodId, phone);
        ModelAndView mav = new ModelAndView();
        mav.addObject("orderInfo", orderInfo);
        mav.setViewName("order");
        return mav;
    }
}
