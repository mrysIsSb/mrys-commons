package top.mrys.auth.token;

import lombok.Data;

/**
 * 账号秘密 token
 *
 * @author mrys
 */
@Data
public class UsernamePwdToken implements Token {

    private boolean valid = false;
    /**
     * 账号
     */
    private String username;
    /**
     * 密码
     */
    private String password;

}
