package top.mrys.example.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import top.mrys.auth.token.UserInfo;

import java.util.List;

/**
 * 自定义用户信息类
 * 扩展了基础的 UserInfo，添加了角色和权限信息
 * 
 * @author mrys
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomUserInfo extends UserInfo {



    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户状态 (ACTIVE, INACTIVE, LOCKED)
     */
    private String status;

    /**
     * 检查用户是否拥有指定角色
     * 
     * @param role 角色名称
     * @return 是否拥有该角色
     */
    public boolean hasRole(String role) {
        return getRoles() != null && getRoles().contains(role);
    }

    /**
     * 检查用户是否拥有指定权限
     * 
     * @param permission 权限名称
     * @return 是否拥有该权限
     */
    public boolean hasPermission(String permission) {
        return getPermissions() != null && getPermissions().contains(permission);
    }

    /**
     * 检查用户是否为管理员
     * 
     * @return 是否为管理员
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 检查用户状态是否为活跃
     * 
     * @return 用户是否活跃
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}