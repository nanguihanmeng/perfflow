package com.perfflow.module.system.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
@Data
public class DeptReq implements Serializable {

    @NotBlank
    // 名称。
    private String name;
    // 父部门ID。
    private Long parentId;
    // 备注。
    private String remark;
}
