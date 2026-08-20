package com.perfflow.common.exception;

import com.perfflow.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常。统一被 {@link GlobalExceptionHandler} 捕获并转换为 {@code Result.fail(...)}。
 */
@Getter
public class BizException extends RuntimeException {

    private final long code;

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }

    public BizException(long code, String message) {
        super(message);
        this.code = code;
    }
}
