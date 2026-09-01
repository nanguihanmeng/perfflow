package com.perfflow.module.auth.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
@Data
public class RefreshReq implements Serializable {

    @NotBlank
    // 刷新令牌。
    private String refreshToken;
}
