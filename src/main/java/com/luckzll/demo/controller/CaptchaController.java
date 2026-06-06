package com.luckzll.demo.controller;

import com.luckzll.demo.utils.CaptchaUtil;
import com.luckzll.demo.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    // 存储验证码（实际生产环境应使用Redis）
    private static final Map<String, String> CAPTCHA_CACHE = new ConcurrentHashMap<>();

    /**
     * 生成验证码
     *
     * @return 验证码图片和key
     */
    @GetMapping("/generate")
    public Result<?> generateCaptcha() {
        // 生成验证码
        CaptchaUtil.CaptchaInfo captchaInfo = CaptchaUtil.generateCaptcha();

        // 生成唯一key
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存储验证码（5分钟有效）
        CAPTCHA_CACHE.put(captchaKey, captchaInfo.getCode());

        // 5分钟后自动删除
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000);
                CAPTCHA_CACHE.remove(captchaKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", captchaInfo.getBase64Image());

        return Result.success(result);
    }

    /**
     * 验证验证码
     *
     * @param captchaKey 验证码key
     * @param captcha    用户输入的验证码
     * @return 是否验证通过
     */
    public static boolean verifyCaptcha(String captchaKey, String captcha) {
        if (captchaKey == null || captcha == null) {
            return false;
        }
        String storedCaptcha = CAPTCHA_CACHE.get(captchaKey);
        if (storedCaptcha == null) {
            return false;
        }
        // 验证后删除
        CAPTCHA_CACHE.remove(captchaKey);
        return storedCaptcha.equalsIgnoreCase(captcha);
    }
}
