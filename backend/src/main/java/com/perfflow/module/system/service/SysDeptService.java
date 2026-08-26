package com.perfflow.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.system.dto.DeptReq;
import com.perfflow.module.system.dto.DeptResp;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDepartmentMapper deptMapper;
    private final SysUserMapper userMapper;

    public List<DeptResp> list() {
        List<SysDepartment> all = deptMapper.selectList(
                new QueryWrapper<SysDepartment>().orderByAsc("sort").orderByAsc("id"));
        List<DeptResp> out = new ArrayList<>();
        for (SysDepartment d : all) out.add(DeptResp.from(d));
        return out;
    }

    @Transactional
    public Long create(DeptReq req) {
        if (deptMapper.selectCount(new QueryWrapper<SysDepartment>().eq("name", req.getName())) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "部门名已存在");
        }
        SysDepartment d = new SysDepartment();
        d.setName(req.getName());
        d.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        d.setRemark(req.getRemark());
        deptMapper.insert(d);
        return d.getId();
    }

    @Transactional
    public void update(Long id, DeptReq req) {
        SysDepartment d = deptMapper.selectById(id);
        if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        // 修改名称时校验重名（排除自身）
        if (StringUtils.hasText(req.getName())) {
            Long dup = deptMapper.selectCount(new QueryWrapper<SysDepartment>()
                    .eq("name", req.getName()).ne("id", id));
            if (dup != null && dup > 0) {
                throw new BizException(ResultCode.BAD_REQUEST, "部门名已存在");
            }
            d.setName(req.getName());
        }
        if (req.getParentId() != null) d.setParentId(req.getParentId());
        if (req.getRemark() != null) d.setRemark(req.getRemark());
        deptMapper.updateById(d);
    }

    @Transactional
    public void delete(Long id) {
        SysDepartment d = deptMapper.selectById(id);
        if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        // 校验是否还有子部门
        if (deptMapper.selectCount(new QueryWrapper<SysDepartment>().eq("parent_id", id)) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "存在子部门，不可删除");
        }
        // 校验部门下是否还有用户
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("dept_id", id)) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "该部门下仍有用户，不可删除");
        }
        deptMapper.deleteById(id);
    }
}
