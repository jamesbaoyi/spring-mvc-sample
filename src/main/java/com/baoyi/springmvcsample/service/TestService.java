package com.baoyi.springmvcsample.service;

import com.baoyi.springmvcsample.annotation.Service;

/**
 * @Author: qijigui
 * @CreateDate: 2019/10/30 15:24
 * @Description:
 */
@Service
public class TestService {

    public String test() {
        return "hello world!";
    }
}

