package com.campushelp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushelp.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 按状态分组统计 */
    @Select("SELECT status, COUNT(*) AS cnt FROM orders GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /** 按类型分组统计 */
    @Select("SELECT order_type, COUNT(*) AS cnt FROM orders GROUP BY order_type")
    List<Map<String, Object>> countByOrderType();
}
