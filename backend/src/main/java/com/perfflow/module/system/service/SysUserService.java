package com.perfflow.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.system.dto.UserCreateReq;
import com.perfflow.module.system.dto.UserResp;
import com.perfflow.module.system.dto.UserUpdateReq;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PWD = "Init@123456";

    public Page<UserResp> page(String username, String role, Long deptId, Integer status,
                               int pageNo, int pageSize) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (StringUtils.hasText(username)) qw.like("username", username);
        if (StringUtils.hasText(role)) qw.eq("role", role);
        if (deptId != null) qw.eq("dept_id", deptId);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");

        Page<SysUser> p = userMapper.selectPage(Page.of(pageNo, pageSize), qw);
        // 部门名缓存
        Map<Long, String> cache = new HashMap<>();
        Page<UserResp> out = Page.of(pageNo, pageSize);
        out.setRecords(new ArrayList<>());
        out.setTotal(p.getTotal());
        for (SysUser u : p.getRecords()) {
            String deptName = null;
            if (u.getDeptId() != null) {
                deptName = cache.computeIfAbsent(u.getDeptId(), id -> {
                    SysDepartment d = deptMapper.selectById(id);
                    return d == null ? null : d.getName();
                });
            }
            out.getRecords().add(UserResp.from(u, deptName));
        }
        return out;
    }

    /**
     * 参与考核的员工选项（HR 开启周期时勾选用）。
     *
     * <p>排除 ADMIN，仅启用状态，含部门名。
     *
     * @return 员工列表
     */
    public List<UserResp> listOptions() {
        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>()
                .ne("role", RoleConst.ROLE_ADMIN)
                .eq("status", 1)
                .orderByAsc("id");
        List<SysUser> users = userMapper.selectList(qw);
        Map<Long, String> cache = new HashMap<>();
        List<UserResp> out = new ArrayList<>(users.size());
        for (SysUser u : users) {
            String deptName = null;
            if (u.getDeptId() != null) {
                deptName = cache.computeIfAbsent(u.getDeptId(), id -> {
                    SysDepartment d = deptMapper.selectById(id);
                    return d == null ? null : d.getName();
                });
            }
            out.add(UserResp.from(u, deptName));
        }
        return out;
    }

    @Transactional
    public Long create(UserCreateReq req) {
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", req.getUsername())) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setRealName(req.getRealName());
        u.setRole(req.getRole());
        u.setDeptId(req.getDeptId());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setStatus(1);
        u.setMustChangePassword(true);
        u.setPassword(passwordEncoder.encode(DEFAULT_PWD));
        // 部门负责人唯一：每个部门至多一名 DEPT_LEAD
        ensureDeptLeadUnique(req.getRole(), req.getDeptId(), null);
        userMapper.insert(u);
        return u.getId();
    }

    @Transactional
    public void update(Long id, UserUpdateReq req) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        if (req.getRealName() != null) u.setRealName(req.getRealName());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getDeptId() != null) u.setDeptId(req.getDeptId());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        // 部门负责人唯一：改为 DEPT_LEAD 时校验该部门
        ensureDeptLeadUnique(req.getRole() != null ? req.getRole() : u.getRole(),
                req.getDeptId() != null ? req.getDeptId() : u.getDeptId(), id);
        userMapper.updateById(u);
    }

    /**
     * 部门负责人唯一校验：每个部门至多一名 DEPT_LEAD 角色用户。
     *
     * @param role     新角色
     * @param deptId   新部门
     * @param excludeId 编辑时排除的自身ID（新增传 null）
     */
    private void ensureDeptLeadUnique(String role, Long deptId, Long excludeId) {
        if (!RoleConst.ROLE_DEPT_LEAD.equals(role) || deptId == null) {
            return;
        }
        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>()
                .eq("role", RoleConst.ROLE_DEPT_LEAD)
                .eq("dept_id", deptId)
                .ne("status", 0);
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        SysUser exist = userMapper.selectOne(qw.last("LIMIT 1"));
        if (exist != null) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "该部门已有部门负责人：" + exist.getRealName());
        }
    }

    @Transactional
    public void delete(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        if ("admin".equals(u.getUsername())) {
            throw new BizException(ResultCode.BAD_REQUEST, "内置管理员不可删除");
        }
        userMapper.deleteById(id);
    }

    @Transactional
    public String resetPassword(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        u.setPassword(passwordEncoder.encode(DEFAULT_PWD));
        u.setMustChangePassword(true);
        userMapper.updateById(u);
        return DEFAULT_PWD;
    }

    @Transactional
    public void toggleStatus(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        u.setStatus(u.getStatus() != null && u.getStatus() == 1 ? 0 : 1);
        userMapper.updateById(u);
    }
}
