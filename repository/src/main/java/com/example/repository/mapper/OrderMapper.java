package com.example.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.repository.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ice
 * @create 2021-05-27 11:00:26
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
