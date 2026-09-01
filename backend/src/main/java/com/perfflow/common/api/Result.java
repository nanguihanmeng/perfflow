package com.perfflow.common.api;
import lombok.Data;
import java.io.Serializable;
// 统一响应体。
@Data
public class Result<T> implements Serializable {

    public static final long SUCCESS_CODE = 0L;
    // 编码
    private long code;
    // 消息
    private String message;
    // 数据
    private T data;
    // 执行 ok。

    public static <T> Result<T> ok() {

        return ok(null);
    }

    // 执行 ok。

    public static <T> Result<T> ok(T data) {

        Result<T> r = new Result<>();
        r.setCode(SUCCESS_CODE);
        r.setMessage("ok");
        r.setData(data);
        return r;
    }

    // 执行 fail。

    public static <T> Result<T> fail(long code, String message) {

        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    // 执行 fail。

    public static <T> Result<T> fail(ResultCode rc) {

        return fail(rc.getCode(), rc.getMessage());
    }

    // 执行 fail。

    public static <T> Result<T> fail(ResultCode rc, String message) {

        return fail(rc.getCode(), message);
    }
}
