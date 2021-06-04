package com.example.controller.handler;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author ice
 * @blog https://blog.csdn.net/dreaming_coder
 * @description
 * @create 2021-06-04 10:18:00
 */
@Controller
public class ErrorHandler {
    @RequestMapping("/404")
    public String error(){
        return "error/404";
    }
}
