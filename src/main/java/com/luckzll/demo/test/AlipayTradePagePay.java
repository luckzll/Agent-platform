package com.luckzll.demo.test;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.response.AlipayTradePagePayResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AlipayTradePagePay {

    public static void main(String[] args) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());

        // 构造请求参数以调用接口
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();

        // 设置商户订单号（使用当前时间戳，避免重复）
        String outTradeNo = "ORDER" + System.currentTimeMillis();
        model.setOutTradeNo(outTradeNo);
        System.out.println("订单号: " + outTradeNo);

        // 设置订单总金额
        model.setTotalAmount("9.99");

        // 设置订单标题
        model.setSubject("测试商品-网页支付");

        // 设置产品码（网页支付固定为 FAST_INSTANT_TRADE_PAY）
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        // 设置订单描述
        model.setBody("这是一个测试订单，用于测试支付宝网页支付功能");

        // 设置超时时间（15分钟）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);
        model.setTimeExpire(expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        request.setBizModel(model);

        // 设置同步回调地址（支付成功后跳转的页面）
        request.setReturnUrl("http://localhost:8080/pay/success");

        // 设置异步通知地址（支付宝服务器回调通知支付结果）
        request.setNotifyUrl("http://localhost:8080/pay/notify");

        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        String form = response.getBody();

        if (response.isSuccess()) {
            System.out.println("调用成功！");
            System.out.println("\n========== 支付表单 HTML ==========\n");
            System.out.println(form);
            System.out.println("\n========== 使用说明 ==========");
            System.out.println("1. 将上面的HTML保存为 .html 文件");
            System.out.println("2. 用浏览器打开该文件");
            System.out.println("3. 点击支付按钮会跳转到支付宝沙箱支付页面");
            System.out.println("4. 使用沙箱账号登录完成支付");
        } else {
            System.out.println("调用失败");
            System.out.println("错误码: " + response.getCode());
            System.out.println("错误信息: " + response.getMsg());
        }
    }

    private static AlipayConfig getAlipayConfig() {
        String privateKey = requireEnvironment("ALIPAY_PRIVATE_KEY");
        String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwOfjPRndqT5cjae9fMMs73Q4cAcJ9InYJElBAXYw+9CNU7m9X4d4K3bGtn/Hm8U2EgoGhsEpQJNp1Mb+1zyEvC3DhzeKixvxC5T3V9RTZ2H0/q0FKgjG0DPtUlzWFAy1puVTKduyBaeTWE2FOpm4B/tu4/Q7c70aau3PV8XwWzz5lIISkF++VkjGgSiZivGPfLs7MpoO4sBu4InDIOd8nmhZkTxMefOmkJ7UwEwbT+bpElOx8vSUi5wPQK+wAtIKxhBadzUm+lGj4zhbvQK12Kz91WH0vwnALB8pRsU6IUqQsp0LXWjpCSyPYbgUJp1D0DjD7506st+uXgItAFF+cQIDAQAB";
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        alipayConfig.setAppId("9021000162642868");
        alipayConfig.setPrivateKey(privateKey);
        alipayConfig.setFormat("json");
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        alipayConfig.setCharset("UTF-8");
        alipayConfig.setSignType("RSA2");
        return alipayConfig;
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required");
        }
        return value;
    }}
