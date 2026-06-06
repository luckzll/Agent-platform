package com.luckzll.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_member")
public class UserMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 会员类型：1-月卡, 2-季卡, 3-年卡, 9-永久 */
    private Integer memberType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态：0-失效, 1-有效 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
