package com.github.acer20246.resourceflow.service;

import com.github.acer20246.resourceflow.dto.UserDTO;
import com.github.acer20246.resourceflow.dto.UserPageQueryDTO;
import com.github.acer20246.resourceflow.entity.User;

import java.util.List;

public interface UserService {
    User findByUserId(Long userId);

    void changeUserStatus(Long userId, Integer status);

    List<User> listUsers(UserPageQueryDTO queryDTO);

    boolean updateUser(Long userId, UserDTO userDTO);

    Boolean changeUserRole(Long userId, Integer role);

    Boolean deleteUser(Long userId);
}
