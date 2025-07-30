package top.mrys.example.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.mrys.auth.annotation.CheckAuth;
import top.mrys.auth.token.TokenContext;
import top.mrys.example.auth.model.CustomUserInfo;
import top.mrys.example.auth.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 演示管理员级别的权限验证功能
 * 所有接口都需要管理员权限
 * 
 * @author mrys
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@CheckAuth("#hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取所有用户列表（仅管理员可访问）
     * 
     * @return 用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<CustomUserInfo> users = userService.getAllUsers();
        
        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("total", users.size());
        
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        log.info("管理员 {} 查看了所有用户列表", currentUser.getUsername());
        
        return ResponseEntity.ok(createResponse(true, "获取用户列表成功", data));
    }

    /**
     * 获取系统统计信息（需要管理员读取权限）
     * 
     * @return 系统统计信息
     */
    @CheckAuth("#hasRole('ADMIN') and #hasPermission('admin:read')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        List<CustomUserInfo> users = userService.getAllUsers();
        
        long activeUsers = users.stream().filter(CustomUserInfo::isActive).count();
        long adminUsers = users.stream().filter(CustomUserInfo::isAdmin).count();
        long lockedUsers = users.stream().filter(user -> "LOCKED".equals(user.getStatus())).count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("activeUsers", activeUsers);
        stats.put("adminUsers", adminUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("systemUptime", System.currentTimeMillis());
        
        Map<String, Object> data = new HashMap<>();
        data.put("stats", stats);
        
        return ResponseEntity.ok(createResponse(true, "获取系统统计信息成功", data));
    }

    /**
     * 锁定用户账户（需要管理员写入权限）
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    @CheckAuth("#hasRole('ADMIN') and #hasPermission('admin:write')")
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<Map<String, Object>> lockUser(@PathVariable String userId) {
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        
        // 不能锁定自己
        if (userId.equals(currentUser.getUserId())) {
            return ResponseEntity.badRequest().body(createResponse(false, "不能锁定自己的账户", null));
        }
        
        CustomUserInfo targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 这里只是演示，实际项目中应该更新数据库
        targetUser.setStatus("LOCKED");
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", targetUser.getUserId());
        data.put("username", targetUser.getUsername());
        data.put("status", targetUser.getStatus());
        
        log.info("管理员 {} 锁定了用户 {}", currentUser.getUsername(), targetUser.getUsername());
        return ResponseEntity.ok(createResponse(true, "锁定用户成功", data));
    }

    /**
     * 解锁用户账户（需要管理员写入权限）
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    @CheckAuth("#hasRole('ADMIN') and #hasPermission('admin:write')")
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<Map<String, Object>> unlockUser(@PathVariable String userId) {
        CustomUserInfo targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 这里只是演示，实际项目中应该更新数据库
        targetUser.setStatus("ACTIVE");
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", targetUser.getUserId());
        data.put("username", targetUser.getUsername());
        data.put("status", targetUser.getStatus());
        
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        log.info("管理员 {} 解锁了用户 {}", currentUser.getUsername(), targetUser.getUsername());
        
        return ResponseEntity.ok(createResponse(true, "解锁用户成功", data));
    }

    /**
     * 重置用户密码（需要管理员写入权限）
     * 
     * @param userId 用户ID
     * @param resetRequest 重置请求
     * @return 操作结果
     */
    @CheckAuth("#hasRole('ADMIN') and #hasPermission('admin:write')")
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable String userId, 
            @RequestBody Map<String, String> resetRequest) {
        
        CustomUserInfo targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }
        
        String newPassword = resetRequest.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "新密码不能为空", null));
        }
        
        // 这里只是演示，实际项目中应该加密密码并更新数据库
        Map<String, Object> data = new HashMap<>();
        data.put("userId", targetUser.getUserId());
        data.put("username", targetUser.getUsername());
        data.put("message", "密码重置成功");
        
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        log.info("管理员 {} 重置了用户 {} 的密码", currentUser.getUsername(), targetUser.getUsername());
        
        return ResponseEntity.ok(createResponse(true, "重置密码成功", data));
    }

    /**
     * 获取系统日志（超级管理员功能）
     * 演示更复杂的权限验证：需要是管理员且拥有特殊权限
     * 
     * @return 系统日志
     */
    @CheckAuth("#hasRole('ADMIN') and #hasPermission('admin:read') and #hasPermission('admin:write')")
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getSystemLogs() {
        // 模拟系统日志数据
        Map<String, Object> logs = new HashMap<>();
        logs.put("loginLogs", "用户登录日志...");
        logs.put("operationLogs", "用户操作日志...");
        logs.put("errorLogs", "系统错误日志...");
        logs.put("auditLogs", "审计日志...");
        
        Map<String, Object> data = new HashMap<>();
        data.put("logs", logs);
        data.put("logCount", 1000);
        data.put("lastUpdate", System.currentTimeMillis());
        
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        log.info("超级管理员 {} 查看了系统日志", currentUser.getUsername());
        
        return ResponseEntity.ok(createResponse(true, "获取系统日志成功", data));
    }

    /**
     * 系统配置管理（超级管理员功能）
     * 
     * @return 系统配置
     */
    @CheckAuth("#isAdmin() and #hasPermission('admin:write')")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxLoginAttempts", 5);
        config.put("sessionTimeout", 3600);
        config.put("passwordPolicy", "强密码策略");
        config.put("enableAuditLog", true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("config", config);
        
        return ResponseEntity.ok(createResponse(true, "获取系统配置成功", data));
    }

    /**
     * 更新系统配置（超级管理员功能）
     * 
     * @param configRequest 配置更新请求
     * @return 操作结果
     */
    @CheckAuth("#isAdmin() and #hasPermission('admin:write')")
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateSystemConfig(@RequestBody Map<String, Object> configRequest) {
        // 这里只是演示，实际项目中应该验证配置并更新数据库
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        
        log.info("超级管理员 {} 更新了系统配置: {}", currentUser.getUsername(), configRequest);
        
        Map<String, Object> data = new HashMap<>();
        data.put("updatedConfig", configRequest);
        data.put("updateTime", System.currentTimeMillis());
        
        return ResponseEntity.ok(createResponse(true, "更新系统配置成功", data));
    }

    /**
     * 创建标准响应格式
     */
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}