package com.luckzll.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("member_order")
public class MemberOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Integer memberType;

    private BigDecimal amount;

    /** 支付渠道：1-支付宝, 2-微信 */
    private Integer payChannel;

    /** 支付状态：0-未支付, 1-已支付, 2-已关闭 */
    private Integer payStatus;

    private LocalDateTime payTime;

    private String transactionId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
