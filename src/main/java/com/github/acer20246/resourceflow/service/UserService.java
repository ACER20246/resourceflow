package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.UserDTO;
import com.github.acer20246.resourceflow.dto.UserPageQueryDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.vo.UserVO;

public interface UserService {
    User findByUserId(Long userId);

    void changeUserStatus(Long userId, Integer status);

    PageResult<UserVO> listUsers(UserPageQueryDTO queryDTO);

    boolean updateUser(Long userId, UserDTO userDTO);

    Boolean changeUserRole(Long userId, Integer role);

    Boolean deleteUser(Long userId);
}
