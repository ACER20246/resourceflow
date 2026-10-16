package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.dto.UserDTO;
import com.github.acer20246.resourceflow.dto.UserPageQueryDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.mapper.UserMapper;
import com.github.acer20246.resourceflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User findByUserId(Long userId) {
        log.info("根据用户id查询用户信息: {}", userId);
        User user=userMapper.selectById(userId);
        if(user==null){
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void changeUserStatus(Long userId, Integer status) {
        log.info("修改用户状态: userId={}, status={}", userId, status);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("用户状态参数无效");
        }
        User user = new User();
        user.setUserId(userId);
        user.setStatus(status == 1 ? "ENABLED" : "DISABLED");
        user.setUpdatedAt(LocalDateTime.now());
        int rows = userMapper.updateById(user);
        if(rows==0){
            throw new BusinessException("用户不存在");
        }
        log.info("用户状态修改成功: userId={}, status={}", userId, status);
    }

    @Override
    public List<User> listUsers(UserPageQueryDTO queryDTO) {
        log.info("分页查询用户: {}", queryDTO);
        long pageNum = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1 : queryDTO.getPageNum();
        long pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(queryDTO.getUsername() != null && !queryDTO.getUsername().isBlank(), User::getUsername, queryDTO.getUsername())
                .eq(queryDTO.getStudentNumber() != null && !queryDTO.getStudentNumber().isBlank(), User::getStudentNumber, queryDTO.getStudentNumber());
        userMapper.selectPage(page, wrapper);
        return page.getRecords();
    }

    @Override
    public boolean updateUser(Long userId, UserDTO userDTO) {
        log.info("修改用户信息: userId={}", userId);
        User user = new User();
        BeanUtils.copyProperties(userDTO,user);
        user.setUserId(userId);
        user.setUpdatedAt(LocalDateTime.now());
        int rows = userMapper.updateById(user);
        if(rows==0){
            throw new BusinessException("用户不存在");
        }
        return true;
    }

    @Override
    public Boolean changeUserRole(Long userId, Integer role) {
        log.info("修改用户角色: userId={}, role={}", userId, role);
        if (role == null || (role != 0 && role != 1)) {
            throw new BusinessException("用户角色参数无效");
        }
        User user = new User();
        user.setUserId(userId);
        if(role==1){
            user.setRole("ADMIN");
        }else{
            user.setRole("USER");
        }
        int rows = userMapper.updateById(user);
        if(rows==0){
            throw new BusinessException("用户不存在");
        }
        return true;
    }

    @Override
    public Boolean deleteUser(Long userId) {
        log.info("删除用户: {}", userId);
        int rows = userMapper.deleteById(userId);
        if(rows==0){
            throw new BusinessException("用户不存在");
        }
        return true;
    }
}
