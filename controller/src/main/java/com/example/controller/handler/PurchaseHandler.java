package com.example.controller.handler;

import com.example.redis.service.PathService;
import com.example.service.service.SecKillService;
import com.example.service.type.ReturnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-03 20:38:36
 */
@Controller
public class PurchaseHandler {
    private PathService pathService;

    private SecKillService secKillService;

    @Autowired
    public void setPathService(PathService pathService) {
        this.pathService = pathService;
    }

    @Autowired
    public void setSecKillService(SecKillService secKillService) {
        this.secKillService = secKillService;
    }

    @ResponseBody
    @GetMapping("/getPathId/{goodId}")
    public String getPathId(@PathVariable("goodId") String goodId) {
        return pathService.getDynamicPath(goodId).toString();
    }

    @RequestMapping("/seckill")
    public ModelAndView purchase(@RequestParam("pathId") String pathId, @RequestParam("goodId") String goodId,
                                 @RequestParam("phone") String phone, @RequestParam("email") String email) {
        ModelAndView mav = new ModelAndView();
        if (!pathService.getDynamicPath(goodId).toString().equals(pathId)) {
            mav.setViewName("redirect:/404");
        } else {
            ReturnType returnType = secKillService.seckill(goodId, phone, email);
            if (returnType == ReturnType.Success){
                mav.addObject("goodId", goodId);
                mav.addObject("phone", phone);
                mav.setViewName("redirect:/order");
            }else {
                mav.setViewName("redirect:/404");
            }
        }
        return mav;
    }

}
