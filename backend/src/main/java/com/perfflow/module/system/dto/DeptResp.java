package com.perfflow.module.system.dto;
import com.perfflow.module.system.entity.SysDepartment;
import lombok.Data;
import java.io.Serializable;
@Data
public class DeptResp implements Serializable {

    // ID。
    private Long id;
    // 名称。
    private String name;
    // 父部门ID。
    private Long parentId;
    // 备注。
    private String remark;
    // 执行 from。

    public static DeptResp from(SysDepartment d) {

        DeptResp r = new DeptResp();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setParentId(d.getParentId());
        r.setRemark(d.getRemark());
        return r;
    }
}
