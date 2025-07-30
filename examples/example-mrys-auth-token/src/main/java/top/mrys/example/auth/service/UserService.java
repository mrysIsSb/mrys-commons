package top.mrys.example.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.mrys.example.auth.model.CustomUserInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务类
 * 提供用户认证、授权等相关功能
 * 注意：这里使用内存数据作为示例，实际项目中应该连接数据库
 *
 * @author mrys
 */
@Slf4j
@Service
public class UserService {

    // 模拟用户数据存储
    private final Map<String, CustomUserInfo> users = new HashMap<>();
    private final Map<String, String> tokenUserMap = new HashMap<>();

    public UserService() {
        initMockData();
    }

    /**
     * 初始化模拟数据
     */
    private void initMockData() {
        // 创建管理员用户
        CustomUserInfo admin = new CustomUserInfo();
        admin.setUserId("1");
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setStatus("ACTIVE");
        admin.setRoles(Set.of("ADMIN", "USER"));
        admin.setPermissions(Set.of("user:read", "user:write", "user:delete", "admin:read", "admin:write"));
        users.put("admin", admin);
        tokenUserMap.put("admin_token", "admin");

        // 创建普通用户
        CustomUserInfo user = new CustomUserInfo();
        user.setUserId("2");
        user.setUsername("user");
        user.setEmail("user@example.com");
        user.setStatus("ACTIVE");
        user.setRoles(Set.of("USER"));
        user.setPermissions(Set.of("user:read"));
        users.put("user", user);
        tokenUserMap.put("user_token", "user");

        // 创建被锁定的用户
        CustomUserInfo lockedUser = new CustomUserInfo();
        lockedUser.setUserId("3");
        lockedUser.setUsername("locked_user");
        lockedUser.setEmail("locked@example.com");
        lockedUser.setStatus("LOCKED");
        lockedUser.setRoles(Set.of("USER"));
        lockedUser.setPermissions(Set.of("user:read"));
        users.put("locked_user", lockedUser);
        tokenUserMap.put("locked_token", "locked_user");

        log.info("初始化模拟用户数据完成，共 {} 个用户", users.size());
    }

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    public CustomUserInfo getUserByUsername(String username) {
        return users.get(username);
    }

    /**
     * 根据 Token 获取用户信息
     *
     * @param token 用户令牌
     * @return 用户信息
     */
    public CustomUserInfo getUserByToken(String token) {
        String username = tokenUserMap.get(token);
        if (username != null) {
            return users.get(username);
        }
        return null;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回 Token，失败返回 null
     */
    public String login(String username, String password) {
        CustomUserInfo user = getUserByUsername(username);
        if (user == null) {
            log.debug("用户不存在: {}", username);
            return null;
        }

        if (!user.isActive()) {
            log.debug("用户状态非活跃: {}", username);
            return null;
        }

        // 简化的密码验证（实际项目中应该使用加密密码）
        if (isValidPassword(username, password)) {
            String token = generateToken(username);
            log.info("用户登录成功: {}", username);
            return token;
        }

        log.debug("密码错误: {}", username);
        return null;
    }

    /**
     * 验证密码
     * 简化实现，实际项目中应该使用加密验证
     */
    private boolean isValidPassword(String username, String password) {
        // 简单的密码规则：密码等于用户名 + "123"
        return (username + "123").equals(password);
    }

    /**
     * 生成 Token
     * 简化实现，实际项目中应该使用 JWT 或其他安全的 Token 生成方式
     */
    private String generateToken(String username) {
        return "Bearer_" + username + "_token";
    }

    /**
     * 获取所有用户列表（仅管理员可访问）
     *
     * @return 用户列表
     */
    public List<CustomUserInfo> getAllUsers() {
        return users.values().stream().toList();
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public CustomUserInfo getUserById(String userId) {
        return users.values().stream()
                .filter(user -> userId.equals(user.getUserId()))
                .findFirst()
                .orElse(null);
    }
}