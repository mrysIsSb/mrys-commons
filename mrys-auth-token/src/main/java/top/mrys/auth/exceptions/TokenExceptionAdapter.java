package top.mrys.auth.exceptions;

import org.springframework.http.ResponseEntity;

import java.util.function.Function;

public interface TokenExceptionAdapter<T> extends Function<TokenException, ResponseEntity<T>> {
}
