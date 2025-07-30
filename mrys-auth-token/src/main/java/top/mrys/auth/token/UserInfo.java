package top.mrys.auth.token;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;

/**
 * 用户信息接口
 * 使用者需要实现此接口来定义自己的用户信息
 *
 * @author mrys
 */
@Setter
@Getter
public class UserInfo {

    /**
     * 获取用户ID
     */
    private String userId;

    /**
     * 获取用户名
     */
    private String username;

    private Set<String> roles = Collections.emptySet();

    private Set<String> permissions = Collections.emptySet();

}