package com.perfflow.common.api;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    public static final long SUCCESS_CODE = 0L;

    private long code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(SUCCESS_CODE);
        r.setMessage("ok");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail(long code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return fail(rc.getCode(), rc.getMessage());
    }

    public static <T> Result<T> fail(ResultCode rc, String message) {
        return fail(rc.getCode(), message);
    }
}
