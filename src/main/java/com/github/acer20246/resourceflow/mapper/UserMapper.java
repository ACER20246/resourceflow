package com.github.acer20246.resourceflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.acer20246.resourceflow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 锁定用户行，防止并发操作。
     *
     * @param userId 用户ID
     * @return 是否成功锁定
     */
    @Select("SELECT * FROM user WHERE user_id =#{userId} FOR UPDATE")
    User lockById(Long userId);
}
