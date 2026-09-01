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
// 系统部门服务：部门列表、创建、修改、删除。
@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDepartmentMapper deptMapper;
    private final SysUserMapper userMapper;

    // 查询列表数据
    public List<DeptResp> list() {

        // 查询列表
        List<SysDepartment> all = deptMapper.selectList(
                new QueryWrapper<SysDepartment>().orderByAsc("sort").orderByAsc("id"));
        // 构建集合容器
        List<DeptResp> out = new ArrayList<>();
        for (SysDepartment d : all) out.add(DeptResp.from(d));
        // 返回结果
        return out;
    }

    @Transactional

    // 创建记录
    public Long create(DeptReq req) {

        // 统计数量
        if (deptMapper.selectCount(new QueryWrapper<SysDepartment>().eq("name", req.getName())) > 0) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "部门名已存在");
        }

        SysDepartment d = new SysDepartment();
        d.setName(req.getName());
        d.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        d.setRemark(req.getRemark());
        // 写入记录
        deptMapper.insert(d);
        return d.getId();
    }

    // 修改部门，改名称时校验重名。
    @Transactional

    // 更新记录
    public void update(Long id, DeptReq req) {

        // 查询单条
        SysDepartment d = deptMapper.selectById(id);
        if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        // 修改名称时校验重名（排除自身）
        if (StringUtils.hasText(req.getName())) {

            // 统计数量
            Long dup = deptMapper.selectCount(new QueryWrapper<SysDepartment>()
                    .eq("name", req.getName()).ne("id", id));

            // 非空才处理
            if (dup != null && dup > 0) {

                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST, "部门名已存在");
            }

            d.setName(req.getName());
        }

        if (req.getParentId() != null) d.setParentId(req.getParentId());
        if (req.getRemark() != null) d.setRemark(req.getRemark());
        // 更新记录
        deptMapper.updateById(d);
    }

    @Transactional

    // 删除记录并清理关联数据
    public void delete(Long id) {

        // 查询单条
        SysDepartment d = deptMapper.selectById(id);
        if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        // 校验是否还有子部门
        if (deptMapper.selectCount(new QueryWrapper<SysDepartment>().eq("parent_id", id)) > 0) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "存在子部门，不可删除");
        }
        // 校验部门下是否还有用户
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("dept_id", id)) > 0) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "该部门下仍有用户，不可删除");
        }
        // 删除记录
        deptMapper.deleteById(id);
    }
}
