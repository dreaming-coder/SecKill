package com.example.controller.handler;

import com.example.redis.service.StockService;
import com.example.repository.entity.Good;
import com.example.service.service.GoodService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-03 09:49:49
 */
@Controller
public class IndexHandler implements InitializingBean {

    private GoodService goodService;
    private StockService stockService;

    @Autowired
    public void setGoodService(GoodService goodService) {
        this.goodService = goodService;
    }

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    @RequestMapping("/list")
    public String index(Model model) {
        List<Good> goodList = goodService.goodList();
        model.addAttribute("goodList", goodList);
        return "list";
    }

    @ResponseBody
    @RequestMapping("/stock/{goodId}")
    public String stock(@PathVariable("goodId") String goodId) {
        return goodService.getStock(goodId).toString();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<Good> goods = goodService.goodList();
        Map<String, Object> map = new HashMap<>();
        goods.forEach(good -> map.put(good.getGoodId(), good.getStock()));
        stockService.initStock(map);
    }
}
