package top.mrys.auth.spel;

import top.mrys.auth.token.TokenContext;
import top.mrys.auth.token.UserInfo;

import java.util.Arrays;
import java.util.Set;

/**
 * SpEL 自定义函数集合
 * 提供常用的权限检查函数
 *
 * @author mrys
 */
public class AuthSpelFunctions {

    /**
     * 检查用户是否拥有指定角色
     *
     * @param role 角色名称
     * @return 是否拥有角色
     */
    public static boolean hasRole(String role) {
        TokenContext context = TokenContext.get();
        if (context == null || context.getUserInfo() == null) {
            return false;
        }

        UserInfo userInfo = context.getUserInfo();
        Set<String> userRoles = userInfo.getRoles();
        return userRoles.contains(role);
    }

    /**
     * 检查用户是否拥有任意一个指定角色
     *
     * @param roles 角色数组
     * @return 是否拥有任意角色
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        TokenContext context = TokenContext.get();
        if (context == null || context.getUserInfo() == null) {
            return false;
        }

        UserInfo userInfo = context.getUserInfo();
        Set<String> userRoles = userInfo.getRoles();

        return Arrays.stream(roles).anyMatch(userRoles::contains);
    }

    /**
     * 检查用户是否拥有指定权限
     *
     * @param permission 权限名称
     * @return 是否拥有权限
     */
    public static boolean hasPermission(String permission) {
        TokenContext context = TokenContext.get();
        if (context == null || context.getUserInfo() == null) {
            return false;
        }

        UserInfo userInfo = context.getUserInfo();
        Set<String> userPermissions = userInfo.getPermissions();
        return userPermissions.contains(permission);
    }

    /**
     * 检查用户是否拥有任意一个指定权限
     *
     * @param permissions 权限数组
     * @return 是否拥有任意权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        TokenContext context = TokenContext.get();
        if (context == null || context.getUserInfo() == null) {
            return false;
        }

        UserInfo userInfo = context.getUserInfo();
        Set<String> userPermissions = userInfo.getPermissions();

        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    /**
     * 检查用户是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        TokenContext context = TokenContext.get();
        return context != null &&
                context.getToken() != null &&
                context.getToken().isValid() &&
                context.getUserInfo() != null;
    }

    /**
     * 检查用户是否为匿名用户
     *
     * @return 是否为匿名用户
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }

    /**
     * 检查用户ID是否匹配
     *
     * @param userId 用户ID
     * @return 是否匹配
     */
    public static boolean hasUserId(String userId) {
        TokenContext context = TokenContext.get();
        if (context == null || context.getUserInfo() == null) {
            return false;
        }

        UserInfo userInfo = context.getUserInfo();
        return userId != null && userId.equals(userInfo.getUserId());
    }

    /**
     * 检查用户名是否匹配
     *
     * @param username 用户名
     * @return 是否匹配
     */
    public static boolean hasUsername(String username) {
        TokenContext context = TokenContext.get();
        if (context == null || context.getUserInfo() == null) {
            return false;
        }

        UserInfo userInfo = context.getUserInfo();
        return username != null && username.equals(userInfo.getUsername());
    }

}