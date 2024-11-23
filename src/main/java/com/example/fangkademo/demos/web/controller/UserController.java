package com.example.fangkademo.demos.web.controller;

import com.example.fangkademo.demos.web.domain.dto.LoginFormDTO;
import com.example.fangkademo.demos.web.domain.entity.Result;
import com.example.fangkademo.demos.web.enums.AppHttpCodeEnum;
import com.example.fangkademo.demos.web.exception.SystemException;
import com.example.fangkademo.demos.web.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class UserController {

    @Autowired
    private LoginService loginService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        //TODO 可以加一个图形码的方式来获取手机号验证码
        if(!StringUtils.hasText(phone)){
            throw new SystemException(AppHttpCodeEnum.PHONENUMBER_NOT_NULL);
        }
        return loginService.sendCode(phone);
    }

    //用户登录
    @PostMapping("/user/login")
    public Result login(@RequestBody LoginFormDTO loginFormDTO){
        if(!StringUtils.hasText(loginFormDTO.getPhone())){
            //提示 必须要传用户名
            throw new RuntimeException("手机号不能为空");
        }
        return loginService.login(loginFormDTO);
    }

    @PostMapping("/user/logout")
    public Result logout(){
        return loginService.logout();
    }

}
