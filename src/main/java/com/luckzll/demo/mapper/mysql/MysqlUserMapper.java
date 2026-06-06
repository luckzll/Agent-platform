package com.luckzll.demo.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luckzll.demo.entity.User;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper (MySQL)
 */
public interface MysqlUserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户
     *
     * @param userPhone 用户手机号
     * @return 用户信息
     */
    User selectByPhone(@Param("userPhone") String userPhone);

    /**
     * 检查手机号是否已存在
     *
     * @param userPhone 用户手机号
     * @return 存在数量
     */
    int countByPhone(@Param("userPhone") String userPhone);
}
