package com.luckzll.demo.controller;

import com.luckzll.demo.entity.dto.UserDTO;
import com.luckzll.demo.service.UserService;
import com.luckzll.demo.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.luckzll.demo.controller.CaptchaController.verifyCaptcha;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int MAX_LOGIN_FAIL_COUNT = 3;
    private static final Duration FAIL_COUNT_EXPIRE = Duration.ofMinutes(30);
    private static final Duration LOGIN_LOCK_EXPIRE = Duration.ofMinutes(30);
    private static final String LOGIN_FAIL_KEY_PREFIX = "auth:login:fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "auth:login:lock:";


    /**
     * 用户注册
     *
     * @param userDTO 用户注册参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody UserDTO userDTO) {
        try {
            // 参数校验
            if (userDTO.getUserPhone() == null || userDTO.getUserPhone().trim().isEmpty()) {
                return Result.badRequest("手机号不能为空");
            }
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                return Result.badRequest("密码不能为空");
            }
            // 简单手机号格式校验
            if (!userDTO.getUserPhone().matches("^1[3-9]\\d{9}$")) {
                return Result.badRequest("手机号格式不正确");
            }
            // 验证码校验
            if (userDTO.getCaptcha() == null || userDTO.getCaptcha().trim().isEmpty()) {
                return Result.badRequest("验证码不能为空");
            }
            if (!verifyCaptcha(userDTO.getCaptchaKey(), userDTO.getCaptcha())) {
                return Result.badRequest("验证码错误或已过期");
            }

            userService.register(userDTO.getUserPhone(), userDTO.getPassword(), userDTO.getUserName());
            return Result.success("注册成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("注册失败：" + e.getMessage());
        }
    }
    /**
     * 用户登录
     *
     * @param userDTO 用户登录参数
     * @return 登录结果（包含Token）
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody UserDTO userDTO) {
        try {
            // 参数校验
            if (userDTO.getUserPhone() == null || userDTO.getUserPhone().trim().isEmpty()) {
                return Result.badRequest("手机号不能为空");
            }
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                return Result.badRequest("密码不能为空");
            }
            // 验证码校验
            if (userDTO.getCaptcha() == null || userDTO.getCaptcha().trim().isEmpty()) {
                return Result.badRequest("验证码不能为空");
            }
            if (!verifyCaptcha(userDTO.getCaptchaKey(), userDTO.getCaptcha())) {
                return Result.badRequest("验证码错误或已过期");
            }

            String userPhone = userDTO.getUserPhone().trim();
            if (isLoginLocked(userPhone)) {
                Long remainMinutes = redisTemplate.getExpire(getLoginLockKey(userPhone), TimeUnit.MINUTES);
                String msg = (remainMinutes != null && remainMinutes > 0)
                        ? "密码错误次数过多，请" + remainMinutes + "分钟后再试"
                        : "密码错误次数过多，请稍后再试";
                return Result.error(429, msg);
            }

            try {
                Map<String, Object> result = userService.login(userPhone, userDTO.getPassword());
                clearLoginFailCache(userPhone);
                return Result.success("登录成功", result);
            } catch (RuntimeException e) {
                increaseLoginFailCount(userPhone);
                if (isLoginLocked(userPhone)) {
                    return Result.error(429, "密码错误次数过多，账号已临时锁定30分钟");
                }
                return Result.error(e.getMessage());
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    private String getLoginFailKey(String userPhone) {
        return LOGIN_FAIL_KEY_PREFIX + userPhone;
    }

    private String getLoginLockKey(String userPhone) {
        return LOGIN_LOCK_KEY_PREFIX + userPhone;
    }

    private boolean isLoginLocked(String userPhone) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getLoginLockKey(userPhone)));
    }

    private void increaseLoginFailCount(String userPhone) {
        String failKey = getLoginFailKey(userPhone);
        Long failCount = redisTemplate.opsForValue().increment(failKey);
        if (failCount != null && failCount == 1) {
            redisTemplate.expire(failKey, FAIL_COUNT_EXPIRE);
        }
        if (failCount != null && failCount >= MAX_LOGIN_FAIL_COUNT) {
            redisTemplate.opsForValue().set(getLoginLockKey(userPhone), "1", LOGIN_LOCK_EXPIRE);
        }
    }

    private void clearLoginFailCache(String userPhone) {
        redisTemplate.delete(getLoginFailKey(userPhone));
        redisTemplate.delete(getLoginLockKey(userPhone));
    }
}
