package com.luckzll.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckzll.demo.entity.User;
import com.luckzll.demo.mapper.mysql.MysqlUserMapper;
import com.luckzll.demo.service.UserService;
import com.luckzll.demo.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<MysqlUserMapper, User> implements UserService {

    @Autowired
    private MysqlUserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public User register(String userPhone, String password, String userName) {
        // 检查手机号是否已存在
        if (userMapper.countByPhone(userPhone) > 0) {
            throw new RuntimeException("手机号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUserPhone(userPhone);
        // 密码MD5加密
        user.setUserPowss(encryptPassword(password));
        user.setUserName(userName != null ? userName : "用户" + userPhone.substring(userPhone.length() - 4));
        user.setUserType(0); // 普通用户

        userMapper.insert(user);
        return user;
    }

    @Override
    public Map<String, Object> login(String userPhone, String password) {
        // 查询用户
        User user = userMapper.selectByPhone(userPhone);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证密码
        if (!user.getUserPowss().equals(encryptPassword(password))) {
            throw new RuntimeException("密码错误");
        }

        // 生成JWT Token
        String token = jwtUtils.generateToken(user.getUserId(), user.getUserName());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("userName", user.getUserName());
        result.put("userPhone", user.getUserPhone());
        result.put("userType", user.getUserType());
        result.put("userTypeName", user.getUserType() == 0 ? "普通用户" : "管理员");
        result.put("token", token);

        return result;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!user.getUserPowss().equals(encryptPassword(oldPassword))) {
            throw new RuntimeException("原密码错误");
        }

        // 更新密码
        user.setUserPowss(encryptPassword(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 密码加密
     *
     * @param password 明文密码
     * @return 加密后的密码
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
