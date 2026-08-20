package com.perfflow.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class DeptReq implements Serializable {

    @NotBlank
    private String name;
    private Long parentId;
    private Long leaderUserId;
    private Integer sort;
    private String remark;
}
