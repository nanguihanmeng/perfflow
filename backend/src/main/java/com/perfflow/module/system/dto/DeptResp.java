package com.perfflow.module.system.dto;

import com.perfflow.module.system.entity.SysDepartment;
import lombok.Data;

import java.io.Serializable;

@Data
public class DeptResp implements Serializable {

    private Long id;
    private String name;
    private Long parentId;
    private String remark;

    public static DeptResp from(SysDepartment d) {
        DeptResp r = new DeptResp();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setParentId(d.getParentId());
        r.setRemark(d.getRemark());
        return r;
    }
}
