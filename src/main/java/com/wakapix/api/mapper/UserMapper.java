package com.wakapix.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wakapix.api.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
