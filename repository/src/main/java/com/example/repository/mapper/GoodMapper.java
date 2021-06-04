package com.example.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.repository.entity.Good;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


/**
 * @author ice
 * @create 2021-05-27 09:37:39
 */
@Mapper
public interface GoodMapper extends BaseMapper<Good> {
    @Update("update sec_goods set stock = stock - 1 ${ew.customSqlSegment}")
    int deliver(@Param(Constants.WRAPPER) Wrapper<Good> wrapper);
}
