package com.perfflow.module.assessment.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;
@Data
public class AssessmentRejectReq implements Serializable {

    @NotBlank
    @Size(max = 500)
    // 意见。
    private String comment;
}
