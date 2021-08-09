package com.example.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.repository.mapper.GoodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-01 18:09:35
 */
@Service
public class GoodService {

    private GoodMapper goodMapper;
    private StockService stockService;

    @Autowired
    public void setGoodMapper(GoodMapper goodMapper) {
        this.goodMapper = goodMapper;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    // 商品页面展示
    public List<Good> goodList() {
        return goodMapper.selectList(null);
    }

    // 商品库存查询
    public Long getStock(String goodId) {
        Long stock = stockService.getStock(goodId);
        if (-1L == stock) {
            QueryWrapper<Good> wrapper = new QueryWrapper<>();
            wrapper.select("stock").eq("good_id", goodId);
            stock = goodMapper.selectOne(wrapper).getStock();
        }
        return stock;
    }

}