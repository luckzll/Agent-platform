package com.luckzll.demo.controller;

import com.luckzll.demo.entity.User;
import com.luckzll.demo.service.UserService;
import com.luckzll.demo.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户信息
     *
     * @param request HTTP请求
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<?> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不返回密码
        user.setUserPowss(null);
        return Result.success(user);
    }

    /**
     * 修改密码
     *
     * @param request HTTP请求
     * @param params  包含 oldPassword 和 newPassword
     * @return 修改结果
     */
    @PostMapping("/updatePassword")
    public Result<?> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        // 参数校验
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.badRequest("原密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.badRequest("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.badRequest("新密码长度不能少于6位");
        }

        try {
            userService.updatePassword(userId, oldPassword, newPassword);
            return Result.success("密码修改成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
