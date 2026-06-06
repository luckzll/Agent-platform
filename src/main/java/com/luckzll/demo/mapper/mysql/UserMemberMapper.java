package com.luckzll.demo.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luckzll.demo.entity.UserMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMemberMapper extends BaseMapper<UserMember> {

    @Select("SELECT * FROM user_member WHERE user_id = #{userId} AND status = 1 AND (end_time IS NULL OR end_time > NOW())")
    UserMember selectValidMemberByUserId(@Param("userId") Long userId);
}
