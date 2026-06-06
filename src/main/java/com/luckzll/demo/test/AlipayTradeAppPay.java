package com.luckzll.demo.test;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.domain.ExtUserInfo;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.domain.GoodsDetail;

import java.util.ArrayList;
import java.util.List;

public class AlipayTradeAppPay {

    public static void main(String[] args) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());

        // 构造请求参数以调用接口
        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        
        // 设置商户订单号
        model.setOutTradeNo("70501111111S001111119");
        
        // 设置订单总金额
        model.setTotalAmount("9.00");
        
        // 设置订单标题
        model.setSubject("大乐透");
        
        // 设置产品码
        model.setProductCode("QUICK_MSECURITY_PAY");
        
        // 设置订单包含的商品列表信息
        List<GoodsDetail> goodsDetail = new ArrayList<GoodsDetail>();
        GoodsDetail goodsDetail0 = new GoodsDetail();
        goodsDetail0.setGoodsName("ipad");
        goodsDetail0.setAlipayGoodsId("20010001");
        goodsDetail0.setQuantity(1L);
        goodsDetail0.setPrice("2000");
        goodsDetail0.setGoodsId("apple-01");
        goodsDetail0.setGoodsCategory("34543238");
        goodsDetail0.setCategoriesTree("124868003|126232002|126252004");
        goodsDetail0.setShowUrl("http://www.alipay.com/xxx.jpg");
        goodsDetail.add(goodsDetail0);
        model.setGoodsDetail(goodsDetail);
        
        // 设置订单绝对超时时间
        model.setTimeExpire("2016-12-31 10:05:00");
        
        // 设置业务扩展参数
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088511833207846");
        extendParams.setHbFqSellerPercent("100");
        extendParams.setHbFqNum("3");
        extendParams.setIndustryRefluxInfo("{\"scene_code\":\"metro_tradeorder\",\"channel\":\"xxxx\",\"scene_data\":{\"asset_name\":\"ALIPAY\"}}");
        extendParams.setRoyaltyFreeze("true");
        extendParams.setCardType("S0JP0000");
        model.setExtendParams(extendParams);
        
        // 设置公用回传参数
        model.setPassbackParams("merchantBizType%3d3C%26merchantBizNo%3d2016010101111");
        
        // 设置商户的原始订单号
        model.setMerchantOrderNo("20161008001");
        
        // 设置外部指定买家
        ExtUserInfo extUserInfo = new ExtUserInfo();
        extUserInfo.setCertType("IDENTITY_CARD");
        extUserInfo.setCertNo("362334768769238881");
        extUserInfo.setMobile("16587658765");
        extUserInfo.setName("李明");
        extUserInfo.setMinAge("18");
        extUserInfo.setNeedCheckInfo("F");
        extUserInfo.setIdentityHash("27bfcd1dee4f22c8fe8a2374af9b660419d1361b1c207e9b41a754a113f38fcc");
        model.setExtUserInfo(extUserInfo);
        
        // 设置通知参数选项
        List<String> queryOptions = new ArrayList<String>();
        queryOptions.add("hyb_amount");
        queryOptions.add("enterprise_pay_info");
        model.setQueryOptions(queryOptions);
        
        request.setBizModel(model);
        // 第三方代调用模式下请设置app_auth_token
        // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);
        String orderStr = response.getBody();
        System.out.println(orderStr);

        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
            // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
            // String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
            // System.out.println(diagnosisUrl);
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