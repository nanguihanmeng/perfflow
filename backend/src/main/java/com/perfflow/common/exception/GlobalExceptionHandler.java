package com.perfflow.common.exception;
import com.perfflow.common.api.Result;
import com.perfflow.common.api.ResultCode;
import com.perfflow.security.DataScopeContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.util.stream.Collectors;
// 全局异常处理：统一输出 {@link Result}。
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    // 执行 handleBiz。

    public ResponseEntity<Result<Void>> handleBiz(BizException ex, HttpServletRequest req) {

        log.warn("biz error: code={}, msg={}, path={}", ex.getCode(), ex.getMessage(), req.getRequestURI());
        // 返回失败响应
        return ResponseEntity.ok(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    // 执行 handleAccessDenied。

    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {

        log.warn("access denied: path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                // 返回失败响应
                .body(Result.fail(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    // 执行 handleAuthN。

    public ResponseEntity<Result<Void>> handleAuthN(AuthenticationException ex, HttpServletRequest req) {

        log.warn("auth error: path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                // 返回失败响应
                .body(Result.fail(ResultCode.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 执行 handleValidation。

    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        // 返回失败响应
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.VALIDATION_FAILED, msg));
    }

    @ExceptionHandler(BindException.class)
    // 执行 handleBind。

    public ResponseEntity<Result<Void>> handleBind(BindException ex) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        // 返回失败响应
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.VALIDATION_FAILED, msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    // 执行 handleMissingParam。

    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException ex) {

        // 返回失败响应
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST,
                "缺少参数: " + ex.getParameterName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    // 执行 handleMethod。

    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                // 返回失败响应
                .body(Result.fail(ResultCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    // 执行 handleNotFound。

    public ResponseEntity<Result<Void>> handleNotFound(NoHandlerFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                // 返回失败响应
                .body(Result.fail(ResultCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    // 执行 handleUnknown。

    public ResponseEntity<Result<Void>> handleUnknown(Exception ex, HttpServletRequest req) {

        log.error("unhandled exception: path={}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                // 返回失败响应
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }

    // 处理 formatFieldError
    private String formatFieldError(FieldError fe) {

        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
