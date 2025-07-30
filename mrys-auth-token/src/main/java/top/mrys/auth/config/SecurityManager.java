package top.mrys.auth.config;

import java.util.ArrayList;
import java.util.List;

/**
 * SecurityManager 是一个安全管理器类，用于处理与安全相关的操作。
 *
 * @author mrys
 */
public class SecurityManager {

    private final List<SecurityConfigWrapper> securityConfigWrappers = new ArrayList<>();

    /**
     * 添加一个 SecurityConfigWrapper 到安全管理器中。
     */
    public SecurityConfigWrapper add(String name) {
        SecurityConfigWrapper wrapper = new SecurityConfigWrapper();
        wrapper.setName(name);
        this.securityConfigWrappers.add(wrapper);
        return wrapper;
    }

    /**
     * 获取所有安全配置包装器
     */
    public List<SecurityConfigWrapper> getSecurityConfigWrappers() {
        return new ArrayList<>(securityConfigWrappers);
    }

    /**
     * 根据名称获取安全配置包装器
     */
    public SecurityConfigWrapper getByName(String name) {
        return securityConfigWrappers.stream()
                .filter(wrapper -> name.equals(wrapper.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 移除指定名称的安全配置包装器
     */
    public boolean remove(String name) {
        return securityConfigWrappers.removeIf(wrapper -> name.equals(wrapper.getName()));
    }

    /**
     * 清空所有安全配置包装器
     */
    public void clear() {
        securityConfigWrappers.clear();
    }
}
