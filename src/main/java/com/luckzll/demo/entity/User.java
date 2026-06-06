package com.luckzll.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户密码
     */
    private String userPowss;

    /**
     * 用户类型0-普通身份，1-会员身份
     */
    private Integer userType;
}
