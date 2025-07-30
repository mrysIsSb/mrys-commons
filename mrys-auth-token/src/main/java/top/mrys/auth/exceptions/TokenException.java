package top.mrys.auth.exceptions;

import lombok.Getter;
import lombok.Setter;
import top.mrys.auth.token.Token;

/**
 * @author mrys
 */
@Getter
@Setter
public class TokenException extends RuntimeException {

    private Token token;

    public TokenException(Token token, String message) {
        super(message);
        this.token = token;
    }

}
