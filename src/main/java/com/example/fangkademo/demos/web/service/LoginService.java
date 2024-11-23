package com.example.fangkademo.demos.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.fangkademo.demos.web.domain.entity.Result;
import com.example.fangkademo.demos.web.domain.entity.User;
import com.example.fangkademo.demos.web.domain.dto.LoginFormDTO;
import com.sun.xml.messaging.saaj.packaging.mime.MessagingException;

import javax.servlet.http.HttpSession;

public interface LoginService extends IService<User> {
    Result login(LoginFormDTO loginForm);


    Result logout();

    Result sendCode(String phone);
}
