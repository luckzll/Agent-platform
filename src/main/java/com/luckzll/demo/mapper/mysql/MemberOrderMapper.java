package com.luckzll.demo.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luckzll.demo.entity.MemberOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberOrderMapper extends BaseMapper<MemberOrder> {

    @Select("SELECT * FROM member_order WHERE order_no = #{orderNo}")
    MemberOrder selectByOrderNo(@Param("orderNo") String orderNo);
}
