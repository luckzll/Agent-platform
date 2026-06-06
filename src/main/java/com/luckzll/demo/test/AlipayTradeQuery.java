package com.luckzll.demo.test;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.response.AlipayTradeQueryResponse;

public class AlipayTradeQuery {

    public static void main(String[] args) throws AlipayApiException {
        // 要查询的订单号（改成你实际支付的订单号）
        String outTradeNo = "ORDER1774537115119";

        AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);

        AlipayTradeQueryResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            System.out.println("========== 订单查询结果 ==========");
            System.out.println("商户订单号: " + response.getOutTradeNo());
            System.out.println("支付宝交易号: " + response.getTradeNo());
            System.out.println("交易状态: " + response.getTradeStatus());
            System.out.println("订单金额: " + response.getTotalAmount());
            System.out.println("买家支付宝账号: " + response.getBuyerLogonId());
            System.out.println("支付时间: " + response.getSendPayDate());

            // 状态说明
            System.out.println("\n========== 状态说明 ==========");
            switch (response.getTradeStatus()) {
                case "WAIT_BUYER_PAY":
                    System.out.println("交易创建，等待买家付款");
                    break;
                case "TRADE_CLOSED":
                    System.out.println("未付款交易超时关闭，或支付完成后全额退款");
                    break;
                case "TRADE_SUCCESS":
                    System.out.println("交易支付成功");
                    break;
                case "TRADE_FINISHED":
                    System.out.println("交易结束，不可退款");
                    break;
                default:
                    System.out.println("未知状态: " + response.getTradeStatus());
            }
        } else {
            System.out.println("查询失败: " + response.getMsg());
            System.out.println("错误码: " + response.getSubCode());
            System.out.println("错误信息: " + response.getSubMsg());
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
