package com.luckzll.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckzll.demo.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userPhone 手机号
     * @param password  密码
     * @param userName  用户名（可选）
     * @return 用户信息
     */
    User register(String userPhone, String password, String userName);

    /**
     * 用户登录
     *
     * @param userPhone 手机号
     * @param password  密码
     * @return 包含用户信息和Token的Map
     */
    Map<String, Object> login(String userPhone, String password);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
