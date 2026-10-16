package com.github.acer20246.resourceflow.controller;

import com.github.acer20246.resourceflow.dto.UserPageQueryDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.service.UserService;
import com.github.acer20246.resourceflow.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@Tag(name = "管理员接口", description = "管理员相关接口")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    public List<UserVO> listUsers(@ParameterObject UserPageQueryDTO queryDTO) {
        log.info("查询用户, queryDTO: {}", queryDTO);
        List<User> users = userService.listUsers(queryDTO);
        return users.stream().map(user -> {
            UserVO userVO = UserVO.builder().build();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "修改用户角色")
    public Boolean changeUserRole(@PathVariable Long userId, @RequestParam Integer role) {
        log.info("修改用户角色, userId: {}, role: {}", userId, role);
        return userService.changeUserRole(userId, role);
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户")
    public Boolean deleteUser(@PathVariable Long userId) {
        log.info("删除用户, userId: {}", userId);
        return userService.deleteUser(userId);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "根据用户id查询用户信息")
    public UserVO findByUserId(@PathVariable Long userId){
        log.info("查询用户信息, userId: {}", userId);
        User user = userService.findByUserId(userId);
        UserVO userVO = UserVO.builder().build();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping("/users/{userId}/status")
    public Boolean changeUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        log.info("修改用户状态, userId: {}, status: {}", userId, status);
        userService.changeUserStatus(userId, status);
        return true;
    }
}
