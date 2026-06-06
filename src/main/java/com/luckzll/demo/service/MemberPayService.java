package com.luckzll.demo.service;

import java.util.Map;

public interface MemberPayService {

    /**
     * 创建支付宝扫码支付订单
     * @param userId 用户ID
     * @param memberType 会员类型
     * @return 二维码链接
     */
    String createAlipayQrCode(Long userId, Integer memberType);

    /**
     * 创建微信支付订单（Native扫码支付）
     * @param userId 用户ID
     * @param memberType 会员类型
     * @return 二维码链接
     */
    String createWechatQrCode(Long userId, Integer memberType);

    /**
     * 处理支付宝回调
     */
    boolean handleAlipayNotify(Map<String, String> params);

    /**
     * 处理微信回调
     */
    boolean handleWechatNotify(String xmlData);

    /**
     * 查询订单支付状态
     */
    Integer queryOrderStatus(String orderNo);

    /**
     * 检查用户是否为有效会员
     */
    boolean isValidMember(Long userId);
}
