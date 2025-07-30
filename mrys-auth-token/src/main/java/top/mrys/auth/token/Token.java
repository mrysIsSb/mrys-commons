package top.mrys.auth.token;

/**
 * Token 接口
 */
public interface Token {

    /**
     * token 是否有效
     */
    boolean isValid();

    void setValid(boolean valid);

}
