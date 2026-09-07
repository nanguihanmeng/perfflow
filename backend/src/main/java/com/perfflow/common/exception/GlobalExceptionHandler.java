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
/**
 * 全局异常处理器：把 Controller 层抛出的各类异常统一转换为 {@link Result} 响应，
 * 并针对业务异常、权限、校验等场景输出不同 HTTP 状态码与错误码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：记录 warn 日志，返回 OK 状态与业务错误码。
     * @param ex 业务异常，携带错误码与提示信息
     * @param req 触发异常的请求，仅用于日志输出
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex, HttpServletRequest req) {

        log.warn("biz error: code={}, msg={}, path={}", ex.getCode(), ex.getMessage(), req.getRequestURI());
        // 返回失败响应
        return ResponseEntity.ok(Result.fail(ex.getCode(), ex.getMessage()));
    }

    /**
     * 访问被拒绝：以 Forbidden 状态返回。
     * @param ex 权限异常
     * @param req 触发异常的请求，仅用于日志输出
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {

        log.warn("access denied: path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                // 返回失败响应
                .body(Result.fail(ResultCode.FORBIDDEN));
    }

    /**
     * 未认证：以 Unauthorized 状态返回认证失败原因。
     * @param ex 认证异常
     * @param req 触发异常的请求，仅用于日志输出
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthN(AuthenticationException ex, HttpServletRequest req) {

        log.warn("auth error: path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                // 返回失败响应
                .body(Result.fail(ResultCode.UNAUTHORIZED, ex.getMessage()));
    }

    /**
     * 请求体字段校验失败：拼接所有字段错误后以 BadRequest 返回。
     * @param ex 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        // 返回失败响应
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.VALIDATION_FAILED, msg));
    }

    /**
     * 表单参数绑定失败：拼接字段错误后以 BadRequest 返回。
     * @param ex 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException ex) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        // 返回失败响应
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.VALIDATION_FAILED, msg));
    }

    /**
     * 缺少必填请求参数：以 BadRequest 返回并提示缺失的参数名。
     * @param ex 缺失参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException ex) {

        // 返回失败响应
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST,
                "缺少参数: " + ex.getParameterName()));
    }

    /**
     * 请求方法不支持：以 MethodNotAllowed 返回。
     * @param ex 方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                // 返回失败响应
                .body(Result.fail(ResultCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 未找到对应处理器：以 NotFound 返回。
     * @param ex 未找到异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoHandlerFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                // 返回失败响应
                .body(Result.fail(ResultCode.NOT_FOUND));
    }

    /**
     * 兜底异常：记录 error 日志后以 InternalServerError 返回，避免把堆栈细节暴露给前端。
     * @param ex 未预期异常
     * @param req 触发异常的请求，仅用于日志输出
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception ex, HttpServletRequest req) {

        log.error("unhandled exception: path={}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                // 返回失败响应
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }

    // 拼接单个字段的校验错误信息，格式为"字段名: 提示"。
    private String formatFieldError(FieldError fe) {

        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
