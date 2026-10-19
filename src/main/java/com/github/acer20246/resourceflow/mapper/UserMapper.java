package com.github.acer20246.resourceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.acer20246.resourceflow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
