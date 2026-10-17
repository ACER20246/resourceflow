package com.github.acer20246.resourceflow.controller;
import com.github.acer20246.resourceflow.dto.UserDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.service.UserService;
import com.github.acer20246.resourceflow.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Tag(name = "用户管理", description = "用户管理相关接口")
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "根据用户id查询用户信息")
    public UserVO findByUserId(@AuthenticationPrincipal Long userId){
        log.info("查询用户信息, userId: {}", userId);
        User user = userService.findByUserId(userId);
        UserVO userVO = UserVO.builder().build();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @PatchMapping
    @Operation(summary = "修改用户信息")
    public Boolean updateUser(@AuthenticationPrincipal Long userId, @RequestBody UserDTO userDTO) {
        log.info("修改用户信息, user: {}", userDTO);
        return userService.updateUser(userId, userDTO);
    }

    @DeleteMapping
    @Operation(summary = "删除用户")
    public Boolean deleteUser(@AuthenticationPrincipal Long userId) {
        log.info("删除用户, userId: {}", userId);
        return userService.deleteUser(userId);
    }
}
