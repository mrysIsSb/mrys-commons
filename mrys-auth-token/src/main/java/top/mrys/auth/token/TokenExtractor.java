package top.mrys.auth.token;

import java.util.Optional;

/**
 * token 提取器
 * @author mrys
 */
public interface TokenExtractor {

    Optional<Token> extract();
}
