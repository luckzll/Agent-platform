package com.luckzll.demo.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.luckzll.demo.entity.MemberOrder;
import com.luckzll.demo.entity.User;
import com.luckzll.demo.entity.UserMember;
import com.luckzll.demo.mapper.mysql.MemberOrderMapper;
import com.luckzll.demo.mapper.mysql.UserMemberMapper;
import com.luckzll.demo.mapper.mysql.MysqlUserMapper;
import com.luckzll.demo.service.MemberPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MemberPayServiceImpl implements MemberPayService {

    @Autowired
    private MemberOrderMapper memberOrderMapper;

    @Autowired
    private UserMemberMapper userMemberMapper;

    @Autowired
    private MysqlUserMapper userMapper;

    @Autowired
    private AlipayClient alipayClient;

    // 支付宝公钥（用于验签）
    private static final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApPYbS9l4BWqjNuIFg5Q5CLAfGuPY+rfLmGxtlhs+m/gjlWY7yau+r+CsmQEiJZaXkFQHsLvSmdIlA03HcKBt+un1xx2D9jHbOZ9mV1+/Kxtmvn+uNu7AHbhL3PBEKPiUiTtHUjl8S0by5qC+W006l8L7ePT9j5ELCD9Mjoto2GeWtCqwz1GyECfu6IKeYaWOZmbOeUPkLGxXn4F4oNxi5RGTsGZp85VAJzJLVvtrFKTLe7XrxAuRbSUC3RLxmzNb7m9u1VAs8ALu9gggg8q4/VsP3A80x3Zjzv6BZ3RiTZPF9MNb7Z29vrAMcdiTcY5IRL8jLqWLKERcxuiSVO0l1wIDAQAB";

    // 会员价格配置
    private BigDecimal getMemberPrice(Integer memberType) {
        return switch (memberType) {
            case 1 -> new BigDecimal("9.90");   // 月卡
            case 2 -> new BigDecimal("25.00");  // 季卡
            case 3 -> new BigDecimal("88.00");  // 年卡
            case 9 -> new BigDecimal("288.00"); // 永久
            default -> new BigDecimal("9.90");
        };
    }

    // 会员标题
    private String getMemberTitle(Integer memberType) {
        return switch (memberType) {
            case 1 -> "月卡会员";
            case 2 -> "季卡会员";
            case 3 -> "年卡会员";
            case 9 -> "永久会员";
            default -> "会员";
        };
    }

    // 计算会员到期时间
    private LocalDateTime calculateEndTime(LocalDateTime startTime, Integer memberType) {
        return switch (memberType) {
            case 1 -> startTime.plus(1, ChronoUnit.MONTHS);   // 月卡
            case 2 -> startTime.plus(3, ChronoUnit.MONTHS);   // 季卡
            case 3 -> startTime.plus(1, ChronoUnit.YEARS);    // 年卡
            case 9 -> null; // 永久
            default -> startTime.plus(1, ChronoUnit.MONTHS);
        };
    }

    @Override
    public String createAlipayQrCode(Long userId, Integer memberType) {
        try {
            // 1. 创建订单
            String orderNo = "M" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
            MemberOrder order = new MemberOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setMemberType(memberType);
            order.setAmount(getMemberPrice(memberType));
            order.setPayChannel(1); // 支付宝
            order.setPayStatus(0);  // 未支付
            memberOrderMapper.insert(order);

            // 2. 调用支付宝接口
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();

            model.setOutTradeNo(orderNo);
            model.setTotalAmount(order.getAmount().toString());
            model.setSubject(getMemberTitle(memberType));
            model.setBody("开通" + getMemberTitle(memberType));

            request.setBizModel(model);
            request.setNotifyUrl("http://www.luckyzll.site/api/pay/alipay/notify");

            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("支付宝扫码支付订单创建成功，订单号：{}，二维码：{}", orderNo, response.getQrCode());
                return response.getQrCode(); // 返回二维码链接
            } else {
                log.error("支付宝订单创建失败：{}，错误码：{}", response.getMsg(), response.getCode());
                throw new RuntimeException("创建支付订单失败：" + response.getMsg());
            }

        } catch (AlipayApiException e) {
            log.error("支付宝接口调用异常", e);
            throw new RuntimeException("支付服务异常");
        }
    }

    @Override
    @Transactional
    public boolean handleAlipayNotify(Map<String, String> params) {
        try {
            log.info("收到支付宝回调，参数：{}", params);

            // 1. 验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                ALIPAY_PUBLIC_KEY,
                "UTF-8",
                "RSA2"
            );

            if (!signVerified) {
                log.error("支付宝回调验签失败");
                return false;
            }

            // 2. 获取订单信息
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String transactionId = params.get("trade_no");

            log.info("订单号：{}，交易状态：{}，支付宝流水号：{}", orderNo, tradeStatus, transactionId);

            // 只处理支付成功的订单
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                log.info("订单未支付成功，当前状态：{}，无需处理", tradeStatus);
                return true;
            }

            // 3. 查询订单
            MemberOrder order = memberOrderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在：{}", orderNo);
                return true;
            }

            // 已处理过的订单直接返回成功
            if (order.getPayStatus() != 0) {
                log.info("订单已处理过，订单号：{}，状态：{}", orderNo, order.getPayStatus());
                return true;
            }

            // 4. 更新订单状态
            order.setPayStatus(1);
            order.setPayTime(LocalDateTime.now());
            order.setTransactionId(transactionId);
            memberOrderMapper.updateById(order);

            // 5. 开通会员
            openMember(order.getUserId(), order.getMemberType());

            log.info("支付宝支付成功处理完成，订单号：{}，用户ID：{}", orderNo, order.getUserId());
            return true;

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return false;
        }
    }

    /**
     * 开通会员（核心逻辑）
     */
    private void openMember(Long userId, Integer memberType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = calculateEndTime(now, memberType);

        // 查询是否已有会员记录
        UserMember existingMember = userMemberMapper.selectValidMemberByUserId(userId);

        if (existingMember != null) {
            // 续费：在原有结束时间上叠加
            LocalDateTime baseTime = existingMember.getEndTime() != null
                && existingMember.getEndTime().isAfter(now)
                ? existingMember.getEndTime() : now;
            endTime = calculateEndTime(baseTime, memberType);

            existingMember.setMemberType(memberType);
            existingMember.setEndTime(endTime);
            existingMember.setStatus(1);
            userMemberMapper.updateById(existingMember);
            log.info("会员续费成功，用户ID：{}，新到期时间：{}", userId, endTime);
        } else {
            // 新开会员
            UserMember member = new UserMember();
            member.setUserId(userId);
            member.setMemberType(memberType);
            member.setStartTime(now);
            member.setEndTime(endTime);
            member.setStatus(1);
            userMemberMapper.insert(member);
            log.info("新会员开通成功，用户ID：{}，到期时间：{}", userId, endTime);
        }

        // 更新user表的userType字段（保持兼容）
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setUserType(1);
            userMapper.updateById(user);
        }
    }

    @Override
    public boolean isValidMember(Long userId) {
        UserMember member = userMemberMapper.selectValidMemberByUserId(userId);
        return member != null;
    }

    @Override
    public Integer queryOrderStatus(String orderNo) {
        MemberOrder order = memberOrderMapper.selectByOrderNo(orderNo);
        return order != null ? order.getPayStatus() : null;
    }

    // ==================== 微信支付部分（待实现） ====================

    @Override
    public String createWechatQrCode(Long userId, Integer memberType) {
        // TODO: 集成微信支付SDK
        throw new UnsupportedOperationException("微信支付开发中，请先使用支付宝支付");
    }

    @Override
    public boolean handleWechatNotify(String xmlData) {
        // TODO: 处理微信支付回调
        throw new UnsupportedOperationException("微信支付开发中");
    }
}
