package com.github.acer20246.resourceflow.mapper;

import com.github.acer20246.resourceflow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthMapper {
    /**
     * 新增用户
     * @param user
     */

    void insert(User user);

    @Select("SELECT * FROM user WHERE username =#{username}")
    User getByUsername(String username);
}
