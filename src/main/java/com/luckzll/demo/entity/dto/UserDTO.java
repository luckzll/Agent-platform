package com.luckzll.demo.entity.dto;

import lombok.Data;

@Data
public class UserDTO {

    /**
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 验证码
     */
    private String captcha;

    /**
     * 验证码key
     */
    private String captchaKey;
}
