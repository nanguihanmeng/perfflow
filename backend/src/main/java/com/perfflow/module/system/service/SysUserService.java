package com.perfflow.module.system.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.system.dto.UserCreateReq;
import com.perfflow.module.system.dto.UserResp;
import com.perfflow.module.system.dto.UserUpdateReq;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 系统用户服务：用户分页查询、参与考核人员选项、创建/修改/删除、重置密码与启用状态切换。
 * 内置绩效考核管理员账号不可被管理员管理；已参与考核的用户禁止删除（可禁用）。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysDepartmentMapper deptMapper;
    private final AssessmentTableMapper assessmentTableMapper;
    private final PasswordEncoder passwordEncoder;
    @Value("${perfflow.default-pwd:12345678}")
    private String defaultPwd;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    // 执行业务处理
    public Page<UserResp> page(String username, String role, Long deptId, Integer status,

                               int pageNo, int pageSize) {

        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        // 绩效考核管理员账号不可见（admin 不能管理）；列表不显示当前登录用户自己
        qw.ne("role", RoleConst.ROLE_PERFORMANCE_HR);
        // 取当前用户上下文
        Long currentUid = DataScopeContext.currentUserId();

        // 非空才处理
        if (currentUid != null) {

            qw.ne("id", currentUid);
        }

        if (StringUtils.hasText(username)) qw.like("username", username);
        if (StringUtils.hasText(role)) qw.eq("role", role);
        if (deptId != null) qw.eq("dept_id", deptId);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");
        // 系统数据访问
        Page<SysUser> p = userMapper.selectPage(Page.of(pageNo, pageSize), qw);
        // 部门名缓存
        Map<Long, String> cache = new HashMap<>();
        // 构建集合容器
        Page<UserResp> out = Page.of(pageNo, pageSize);
        out.setRecords(new ArrayList<>());
        out.setTotal(p.getTotal());

        for (SysUser u : p.getRecords()) {

            String deptName = null;

            // 非空才处理
            if (u.getDeptId() != null) {

                deptName = cache.computeIfAbsent(u.getDeptId(), id -> {

                    // 查询单条
                    SysDepartment d = deptMapper.selectById(id);
                    return d == null ? null : d.getName();
                });
            }

            out.getRecords().add(UserResp.from(u, deptName));
        }

        // 返回结果
        return out;
    }

    // 参与考核的员工选项（绩效考核管理员开启周期时勾选用）。
    // 参与考核的用户选项（HR 开启个人线周期勾选用）。

    // 查询可选填报项
    public List<UserResp> listOptions(List<String> roles) {

        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>()
                .ne("role", RoleConst.ROLE_ADMIN)
                .ne("role", RoleConst.ROLE_PERFORMANCE_HR)
                .eq("status", STATUS_ENABLED)
                .orderByAsc("id");

        // 非空才处理
        if (roles != null && !roles.isEmpty()) {

            qw.in("role", roles);
        }

        // 查询列表
        List<SysUser> users = userMapper.selectList(qw);
        // 构建集合容器
        Map<Long, String> cache = new HashMap<>();
        // 构建集合容器
        List<UserResp> out = new ArrayList<>(users.size());

        for (SysUser u : users) {

            String deptName = null;

            // 非空才处理
            if (u.getDeptId() != null) {

                deptName = cache.computeIfAbsent(u.getDeptId(), id -> {

                    // 查询单条
                    SysDepartment d = deptMapper.selectById(id);
                    return d == null ? null : d.getName();
                });
            }

            out.add(UserResp.from(u, deptName));
        }

        // 返回结果
        return out;
    }

    @Transactional

    // 创建记录
    public Long create(UserCreateReq req) {

        // 统计数量
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", req.getUsername())) > 0) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "用户名已存在");
        }

        // 角色判断
        if (RoleConst.ROLE_PERFORMANCE_HR.equals(req.getRole())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "绩效考核管理员账号不可由管理员创建，仅由系统维护");
        }

        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setRealName(req.getRealName());
        u.setRole(req.getRole());
        u.setDeptId(req.getDeptId());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setStatus(STATUS_ENABLED);
        // 标记需改密
        u.setMustChangePassword(true);
        // 密码加密
        u.setPassword(passwordEncoder.encode(defaultPwd));
        // 部门负责人唯一：每个部门至多一名 DEPT_LEAD
        ensureDeptLeadUnique(req.getRole(), req.getDeptId(), null);
        // 写入记录
        userMapper.insert(u);
        return u.getId();
    }

    @Transactional

    // 更新记录
    public void update(Long id, UserUpdateReq req) {

        // 查询单条
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        // 权限或数据校验
        assertNotSelf(id, "修改");
        // 权限或数据校验
        assertNotPerformanceHr(u, "修改");

        // 非空才处理
        if (req.getRole() != null && RoleConst.ROLE_PERFORMANCE_HR.equals(req.getRole())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "绩效考核管理员角色不可由管理员设置");
        }

        if (req.getRealName() != null) u.setRealName(req.getRealName());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getDeptId() != null) u.setDeptId(req.getDeptId());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        // 设置状态
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        // 部门负责人唯一：改为 DEPT_LEAD 时校验该部门
        ensureDeptLeadUnique(req.getRole() != null ? req.getRole() : u.getRole(),
                req.getDeptId() != null ? req.getDeptId() : u.getDeptId(), id);
        // 更新记录
        userMapper.updateById(u);
    }

    // 校验目标用户非绩效考核管理员（admin 不可管理）。

    private void assertNotPerformanceHr(SysUser u, String action) {

        // 角色判断
        if (RoleConst.ROLE_PERFORMANCE_HR.equals(u.getRole())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "绩效考核管理员账号不可" + action + "，仅可登录后自行修改资料");
        }
    }

    // 禁止操作当前登录用户自己的账号（不能删除/禁用/修改/重置自己）。

    private void assertNotSelf(Long id, String action) {

        // 取当前用户上下文
        Long currentUid = DataScopeContext.currentUserId();

        // 非空才处理
        if (currentUid != null && currentUid.equals(id)) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "不能" + action + "自己的账号");
        }
    }

    // 部门负责人唯一校验：每个部门至多一名 DEPT_LEAD 角色用户。

    private void ensureDeptLeadUnique(String role, Long deptId, Long excludeId) {

        // 判空处理
        if (!RoleConst.ROLE_DEPT_LEAD.equals(role) || deptId == null) {

            return;
        }

        QueryWrapper<SysUser> qw = new QueryWrapper<SysUser>()
                .eq("role", RoleConst.ROLE_DEPT_LEAD)
                .eq("dept_id", deptId)
                .ne("status", 0);

        // 非空才处理
        if (excludeId != null) {

            qw.ne("id", excludeId);
        }

        // 查询单条
        SysUser exist = userMapper.selectOne(qw.last("LIMIT 1"));

        // 非空才处理
        if (exist != null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "该部门已有部门负责人：" + exist.getRealName());
        }
    }

    @Transactional

    // 删除记录并清理关联数据
    public void delete(Long id) {

        // 查询单条
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        // 权限或数据校验
        assertNotSelf(id, "删除");

        // 相等判断
        if ("admin".equals(u.getUsername())) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "内置管理员不可删除");
        }

        // 权限或数据校验
        assertNotPerformanceHr(u, "删除");
        // 已参与考核的用户保留历史数据，禁止删除
        Long tableCount = assessmentTableMapper.selectCount(new QueryWrapper<AssessmentTable>()
                .eq("user_id", id));
        if (tableCount != null && tableCount > 0) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "该用户已参与考核并存在考核记录，不可删除，可改用禁用账号");
        }

        // 删除记录
        userMapper.deleteById(id);
    }

    @Transactional
    // 重置password。

    // 重置密码为默认值
    public String resetPassword(Long id) {

        // 查询单条
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        // 权限或数据校验
        assertNotSelf(id, "重置密码");
        // 权限或数据校验
        assertNotPerformanceHr(u, "重置密码");
        // 密码加密
        u.setPassword(passwordEncoder.encode(defaultPwd));
        // 标记需改密
        u.setMustChangePassword(true);
        // 更新记录
        userMapper.updateById(u);
        return defaultPwd;
    }

    @Transactional
    // 切换status。

    // 切换启用状态
    public void toggleStatus(Long id) {

        // 查询单条
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.NOT_FOUND);
        // 权限或数据校验
        assertNotSelf(id, "禁用/启用");
        // 权限或数据校验
        assertNotPerformanceHr(u, "禁用/启用");
        u.setStatus(u.getStatus() != null && STATUS_ENABLED == u.getStatus()
                ? STATUS_DISABLED : STATUS_ENABLED);
        // 更新记录
        userMapper.updateById(u);
    }
}
