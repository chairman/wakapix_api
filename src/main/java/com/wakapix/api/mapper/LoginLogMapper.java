package com.wakapix.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wakapix.api.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
