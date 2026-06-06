package com.luckzll.demo.controller;

import com.luckzll.demo.service.MemberPayService;
import com.luckzll.demo.utils.JwtUtils;
import com.luckzll.demo.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private MemberPayService memberPayService;

    /**
     * 创建支付宝扫码支付订单
     * @param memberType 会员类型：1-月卡, 2-季卡, 3-年卡, 9-永久
     */
    @PostMapping("/alipay/qrcode")
    public Result<Map<String, String>> createAlipayQrCode(
            @RequestParam Integer memberType,
            @RequestHeader("Authorization") String token) {
        try {
            // 从token解析用户ID
            Long userId = JwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
            if (userId == null) {
                return Result.error("请先登录");
            }

            String qrCodeUrl = memberPayService.createAlipayQrCode(userId, memberType);

            Map<String, String> result = new HashMap<>();
            result.put("qrCodeUrl", qrCodeUrl);

            log.info("创建支付宝支付订单成功，用户ID：{}，会员类型：{}", userId, memberType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建支付宝订单失败", e);
            return Result.error("创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 支付宝支付回调（支付宝服务器调用）
     * 注意：此接口不需要登录验证
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        try {
            // 获取支付宝回调参数
            Map<String, String> params = new HashMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                String[] values = request.getParameterValues(name);
                String valueStr = String.join(",", values);
                params.put(name, valueStr);
            }

            log.info("收到支付宝回调请求，参数：{}", params);

            boolean success = memberPayService.handleAlipayNotify(params);

            // 必须返回 "success" 或 "fail"
            return success ? "success" : "fail";
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return "fail";
        }
    }

    /**
     * 查询订单支付状态（前端轮询用）
     * @param orderNo 订单号
     */
    @GetMapping("/order/status")
    public Result<Integer> queryOrderStatus(@RequestParam String orderNo) {
        try {
            Integer status = memberPayService.queryOrderStatus(orderNo);
            if (status == null) {
                return Result.error("订单不存在");
            }
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询订单状态失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 检查当前用户是否为有效会员
     */
    @GetMapping("/member/check")
    public Result<Map<String, Object>> checkMemberStatus(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
            if (userId == null) {
                return Result.error("请先登录");
            }

            boolean isMember = memberPayService.isValidMember(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("isMember", isMember);

            return Result.success(result);
        } catch (Exception e) {
            log.error("检查会员状态失败", e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 创建微信支付订单（预留接口）
     */
    @PostMapping("/wechat/qrcode")
    public Result<Map<String, String>> createWechatQrCode(
            @RequestParam Integer memberType,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
            if (userId == null) {
                return Result.error("请先登录");
            }

            String qrCodeUrl = memberPayService.createWechatQrCode(userId, memberType);

            Map<String, String> result = new HashMap<>();
            result.put("qrCodeUrl", qrCodeUrl);

            return Result.success(result);
        } catch (Exception e) {
            log.error("创建微信订单失败", e);
            return Result.error("创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 微信支付回调（预留接口）
     */
    @PostMapping("/wechat/notify")
    public String wechatNotify(@RequestBody String xmlData) {
        try {
            log.info("收到微信回调");
            boolean success = memberPayService.handleWechatNotify(xmlData);
            return success ? "<xml><return_code><![CDATA[SUCCESS]]></return_code></xml>"
                          : "<xml><return_code><![CDATA[FAIL]]></return_code></xml>";
        } catch (Exception e) {
            log.error("处理微信回调异常", e);
            return "<xml><return_code><![CDATA[FAIL]]></return_code></xml>";
        }
    }
}
