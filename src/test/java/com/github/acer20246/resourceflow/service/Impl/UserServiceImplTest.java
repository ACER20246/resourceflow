package com.github.acer20246.resourceflow.service.Impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.acer20246.resourceflow.common.exception.BusinessException;
import com.github.acer20246.resourceflow.common.result.PageResult;
import com.github.acer20246.resourceflow.dto.UserDTO;
import com.github.acer20246.resourceflow.dto.UserPageQueryDTO;
import com.github.acer20246.resourceflow.entity.User;
import com.github.acer20246.resourceflow.mapper.UserMapper;
import com.github.acer20246.resourceflow.vo.UserVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserServiceImpl} 单元测试：覆盖用户查询、状态/角色修改、分页查询与删除。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    /**
     * MyBatis-Plus 的 Lambda 条件依赖实体的 TableInfo 缓存，
     * 纯单元测试环境中需要手动初始化。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(10001L);
        user.setUsername("zhangsan");
        user.setStudentNumber("2026001");
        user.setRole("USER");
        user.setStatus("ENABLED");
        return user;
    }

    @Test
    @DisplayName("根据 ID 查询用户：存在时返回用户信息")
    void findByUserIdSuccess() {
        // arrange
        when(userMapper.selectById(10001L)).thenReturn(buildUser());

        // act
        User user = userService.findByUserId(10001L);

        // assert
        assertThat(user.getUserId()).isEqualTo(10001L);
        assertThat(user.getUsername()).isEqualTo("zhangsan");
    }

    @Test
    @DisplayName("根据 ID 查询用户：不存在时抛出业务异常")
    void findByUserIdNotFound() {
        // arrange
        when(userMapper.selectById(999L)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> userService.findByUserId(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    @Test
    @DisplayName("修改用户状态：参数为空或非法时抛出业务异常")
    void changeUserStatusWithInvalidParam() {
        // act & assert
        assertThatThrownBy(() -> userService.changeUserStatus(10001L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户状态参数无效");
        assertThatThrownBy(() -> userService.changeUserStatus(10001L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户状态参数无效");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("修改用户状态：1 表示启用，0 表示禁用")
    void changeUserStatusSuccess() {
        // arrange
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // act
        userService.changeUserStatus(10001L, 1);
        userService.changeUserStatus(10001L, 0);

        // assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo("ENABLED");
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo("DISABLED");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("修改用户状态：用户不存在时抛出业务异常")
    void changeUserStatusUserNotFound() {
        // arrange
        when(userMapper.updateById(any(User.class))).thenReturn(0);

        // act & assert
        assertThatThrownBy(() -> userService.changeUserStatus(999L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    @Test
    @DisplayName("分页查询用户：分页参数为空时默认第 1 页、每页 10 条")
    void listUsersWithDefaultPageParams() {
        // arrange：模拟 MyBatis-Plus 分页查询回填数据
        doAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(buildUser()));
            page.setTotal(1);
            return page;
        }).when(userMapper).selectPage(any(), any());

        // act
        PageResult<UserVO> result = userService.listUsers(new UserPageQueryDTO());

        // assert
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(userMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10L);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).getUsername()).isEqualTo("zhangsan");
        assertThat(result.page()).isEqualTo(1L);
        assertThat(result.pageSize()).isEqualTo(10L);
        assertThat(result.total()).isEqualTo(1L);
    }

    @Test
    @DisplayName("分页查询用户：携带用户名与学号条件时按条件过滤")
    void listUsersWithConditions() {
        // arrange
        UserPageQueryDTO queryDTO = UserPageQueryDTO.builder()
                .username("zhang")
                .studentNumber("2026001")
                .pageNum(2)
                .pageSize(5)
                .build();
        doAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        }).when(userMapper).selectPage(any(), any());

        // act
        PageResult<UserVO> result = userService.listUsers(queryDTO);

        // assert
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(userMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5L);
        assertThat(result.records()).isEmpty();
        assertThat(result.total()).isZero();
    }

    @Test
    @DisplayName("修改用户信息：用户不存在时抛出业务异常")
    void updateUserUserNotFound() {
        // arrange
        when(userMapper.updateById(any(User.class))).thenReturn(0);

        // act & assert
        assertThatThrownBy(() -> userService.updateUser(999L, new UserDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    @Test
    @DisplayName("修改用户信息：成功时返回 true 并更新时间戳")
    void updateUserSuccess() {
        // arrange
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("lisi");

        // act
        boolean updated = userService.updateUser(10001L, userDTO);

        // assert
        assertThat(updated).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(10001L);
        assertThat(captor.getValue().getUsername()).isEqualTo("lisi");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("修改用户角色：参数非法时抛出业务异常")
    void changeUserRoleWithInvalidParam() {
        // act & assert
        assertThatThrownBy(() -> userService.changeUserRole(10001L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户角色参数无效");
        assertThatThrownBy(() -> userService.changeUserRole(10001L, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户角色参数无效");
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("修改用户角色：1 设置为 ADMIN，0 设置为 USER")
    void changeUserRoleSuccess() {
        // arrange
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // act
        userService.changeUserRole(10001L, 1);
        userService.changeUserRole(10001L, 0);

        // assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues().get(0).getRole()).isEqualTo("ADMIN");
        assertThat(captor.getAllValues().get(1).getRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("删除用户：用户不存在时抛出业务异常")
    void deleteUserNotFound() {
        // arrange
        when(userMapper.deleteById(999L)).thenReturn(0);

        // act & assert
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    @Test
    @DisplayName("删除成功：返回 true")
    void deleteUserSuccess() {
        // arrange
        when(userMapper.deleteById(10001L)).thenReturn(1);

        // act & assert
        assertThat(userService.deleteUser(10001L)).isTrue();
    }
}