package com.baoyi.springmvcsample.controller;

import com.baoyi.springmvcsample.annotation.Autowired;
import com.baoyi.springmvcsample.annotation.RequestMapping;
import com.baoyi.springmvcsample.annotation.RestController;
import com.baoyi.springmvcsample.service.TestService;

/**
 * @Author: qijigui
 * @CreateDate: 2019/10/30 15:22
 * @Description:
 */
@RestController
@RequestMapping("/testController")
public class TestController {

    @Autowired
    TestService testService;

    @RequestMapping(value = "/index")
    public void index() {
        System.out.println(testService.test());
    }
}
