package top.mrys.example.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.mrys.auth.annotation.Anno;
import top.mrys.auth.annotation.CheckAuth;
import top.mrys.auth.annotation.RequireLogin;
import top.mrys.auth.token.TokenContext;
import top.mrys.example.auth.model.CustomUserInfo;
import top.mrys.example.auth.service.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 演示 mrys-auth-token 的基本权限验证功能
 * 
 * @author mrys
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录接口（无需权限验证）
     * 
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    @Anno
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(createResponse(false, "用户名和密码不能为空", null));
        }
        
        String token = userService.login(username, password);
        if (token != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", username);
            
            log.info("用户登录成功: {}", username);
            return ResponseEntity.ok(createResponse(true, "登录成功", data));
        } else {
            log.warn("用户登录失败: {}", username);
            return ResponseEntity.status(401).body(createResponse(false, "用户名或密码错误", null));
        }
    }

    /**
     * 获取用户信息（需要登录）
     * 
     * @return 用户信息
     */
    @RequireLogin
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        try {
            CustomUserInfo userInfo = (CustomUserInfo) TokenContext.get().getUserInfo();
            if (userInfo != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("userId", userInfo.getUserId());
                data.put("username", userInfo.getUsername());
                data.put("roles", userInfo.getRoles());
                data.put("permissions", userInfo.getPermissions());
                return ResponseEntity.ok(createResponse(true, "获取用户信息成功", data));
            } else {
                return ResponseEntity.status(401).body(createResponse(false, "用户未登录", null));
            }
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseEntity.status(500).body(createResponse(false, "获取用户信息失败", null));
        }
    }

    /**
     * 更新用户信息（需要用户写入权限）
     * 
     * @param updateRequest 更新请求
     * @return 更新结果
     */
    @CheckAuth("#hasPermission('user:write')")
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> updateRequest) {
        TokenContext context = TokenContext.get();
        CustomUserInfo userInfo = (CustomUserInfo) context.getUserInfo();
        
        // 这里只是演示，实际项目中应该更新数据库
        String newEmail = updateRequest.get("email");
        if (newEmail != null) {
            userInfo.setEmail(newEmail);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userInfo.getUserId());
        data.put("username", userInfo.getUsername());
        data.put("email", userInfo.getEmail());
        
        log.info("用户 {} 更新了个人信息", userInfo.getUsername());
        return ResponseEntity.ok(createResponse(true, "更新用户信息成功", data));
    }

    /**
     * 获取用户详细信息（需要用户读取权限）
     * 
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @CheckAuth("#hasPermission('user:read')")
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String userId) {
        CustomUserInfo userInfo = userService.getUserById(userId);
        
        if (userInfo == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userInfo.getUserId());
        data.put("username", userInfo.getUsername());
        data.put("email", userInfo.getEmail());
        data.put("status", userInfo.getStatus());
        data.put("roles", userInfo.getRoles());
        
        return ResponseEntity.ok(createResponse(true, "获取用户信息成功", data));
    }

    /**
     * 删除用户（需要用户删除权限）
     * 
     * @param userId 用户ID
     * @return 删除结果
     */
    @CheckAuth("#hasPermission('user:delete')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String userId) {
        TokenContext context = TokenContext.get();
        CustomUserInfo currentUser = (CustomUserInfo) context.getUserInfo();
        
        // 不能删除自己
        if (userId.equals(currentUser.getUserId())) {
            return ResponseEntity.badRequest().body(createResponse(false, "不能删除自己的账户", null));
        }
        
        CustomUserInfo userInfo = userService.getUserById(userId);
        if (userInfo == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 这里只是演示，实际项目中应该从数据库删除
        log.info("用户 {} 删除了用户 {}", currentUser.getUsername(), userInfo.getUsername());
        return ResponseEntity.ok(createResponse(true, "删除用户成功", null));
    }

    /**
     * 测试复杂权限表达式（需要管理员角色或用户写入权限）
     * 
     * @return 测试结果
     */
    @CheckAuth("#hasRole('ADMIN') or #hasPermission('user:write')")
    @PostMapping("/test-complex-auth")
    public ResponseEntity<Map<String, Object>> testComplexAuth() {
        TokenContext context = TokenContext.get();
        CustomUserInfo userInfo = (CustomUserInfo) context.getUserInfo();
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "复杂权限验证通过");
        data.put("user", userInfo.getUsername());
        data.put("hasAdminRole", userInfo.hasRole("ADMIN"));
        data.put("hasUserWritePermission", userInfo.hasPermission("user:write"));
        
        return ResponseEntity.ok(createResponse(true, "复杂权限验证成功", data));
    }

    /**
     * 公开接口（无需任何权限）
     * 
     * @return 公开信息
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "这是一个公开接口，无需任何权限即可访问");
        data.put("timestamp", System.currentTimeMillis());
        data.put("version", "1.0.0");
        
        return ResponseEntity.ok(createResponse(true, "获取公开信息成功", data));
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