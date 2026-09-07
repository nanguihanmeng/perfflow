package com.perfflow.module.assessment.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.enums.RowCategory;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 个人考核数据权限与字段脱敏判定服务：依据当前用户角色/部门，决定某张考核表是否可见、
 * 某行可否编辑、得分字段是否脱敏，并为列表查询叠加可见性条件。
 */
@Service
@RequiredArgsConstructor
public class AssessmentPermissionService {

    private final AssessmentTableMapper tableMapper;
    private final SysUserMapper userMapper;

    // 执行业务处理
    public String currentRole() {

        // 取当前用户上下文
        return DataScopeContext.current().getPrimaryRole();
    }

    // 执行 isEmp。

    // 执行业务处理
    public boolean isEmp()     { return RoleConst.ROLE_EMP.equals(currentRole()); }
    // 执行 isDeptLead。

    // 执行业务处理
    public boolean isDeptLead(){ return RoleConst.ROLE_DEPT_LEAD.equals(currentRole()); }
    // 执行 isLead。

    // 执行业务处理
    public boolean isLead()    { return RoleConst.ROLE_LEAD.equals(currentRole()); }
    // 执行 isHr。

    // 执行业务处理
    public boolean isHr()      { return RoleConst.ROLE_PERFORMANCE_HR.equals(currentRole()); }

    /**
     * 校验当前用户对指定考核表是否可见，不可见直接抛 FORBIDDEN。
     * 自评挂起(待人事推送)阶段仅 HR 与本人可见；其余按角色规则放行。
     * @param t 个人考核主表
     */
    public void ensureVisible(AssessmentTable t) {

        String role = currentRole();

        // 判空处理
        if (role == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 自评挂起(待人事推送)阶段：仅 HR 与本人可见，其余角色不可见
        if (AssessmentState.SELF_SUSPENDED.name().equals(t.getState())) {

            // 角色判断
            if (RoleConst.ROLE_PERFORMANCE_HR.equals(role)) {

                return;
            }

            // 条件分支
            if (isAssessed(role)) {

                // 取当前用户上下文
                Long uid = DataScopeContext.currentUserId();

                // 非空才处理
                if (uid != null && uid.equals(t.getUserId())) {

                    return;
                }
            }

            // 校验失败抛异常
            throw new BizException(ResultCode.FORBIDDEN);
        }

        // 按角色或类型分发
        switch (role) {

            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { /* all */ }

            case RoleConst.ROLE_COMMITTEE -> {
                // 委员会可见 LEAD 的表（评分对象）+ 全公司已完成结果
                if (isLeadTable(t)) {

                    return;
                }

                // 状态判断
                if (AssessmentState.FINISHED.name().equals(t.getState())) {

                    return;
                }

                // 校验失败抛异常
                throw new BizException(ResultCode.FORBIDDEN);
            }

            case RoleConst.ROLE_DEPT_LEAD -> {

                // 取当前用户上下文
                Long deptId = DataScopeContext.currentDeptId();
                boolean own = isOwn(t);

                // 非空才处理
                if (deptId != null && deptId.equals(t.getDeptId())) {

                    return;
                }

                // 条件分支
                if (own) {

                    return;
                }

                // 校验失败抛异常
                throw new BizException(ResultCode.FORBIDDEN);
            }

            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION -> {

                // 条件分支
                if (isOwn(t)) {

                    return;
                }

                // 校验失败抛异常
                throw new BizException(ResultCode.FORBIDDEN);
            }

            default -> throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /**
     * 判断当前用户能否编辑指定行：员工仅在自评中可编辑本人普通(非加减分)行，
     * 部门领导仅在待部门审核阶段可编辑本部门全部行。
     * @param t 个人考核主表
     * @param r 目标指标行
     */
    public boolean canEditRow(AssessmentTable t, AssessmentRow r) {

        String role = currentRole();
        if (role == null) return false;
        AssessmentState cur = AssessmentState.valueOf(t.getState());
        boolean isPlanOrOpen = r.getCategory() != null && !RowCategory.BONUS.name().equals(r.getCategory());

        return switch (role) {

            case RoleConst.ROLE_EMP -> {

                // 取当前用户上下文
                boolean isMine = DataScopeContext.currentUserId() != null
                        // 取当前用户上下文
                        && DataScopeContext.currentUserId().equals(t.getUserId());
                // 员工不可填加减分项
                yield isMine && cur == AssessmentState.SELF_DRAFTING && isPlanOrOpen;
            }

            case RoleConst.ROLE_DEPT_LEAD -> {

                // 取当前用户上下文
                boolean sameDept = DataScopeContext.currentDeptId() != null
                        // 取当前用户上下文
                        && DataScopeContext.currentDeptId().equals(t.getDeptId());
                // 部门领导可编辑全部行（含加减分项）
                yield sameDept && cur == AssessmentState.DEPT_REVIEW;
            }

            default -> false;
        };
    }

    /**
     * 判断该考核表对当前用户是否需要对得分类字段脱敏：
     * 员工仅本人表可见分；部门领导可看本人与本部门；委员会仅对非 LEAD 的表脱敏。
     * @param t 个人考核主表
     */
    public boolean isRowMasked(AssessmentTable t) {

        String role = currentRole();
        if (role == null) return true;
        // 按角色或类型分发
        switch (role) {
            // LEAD / HR / COMMITTEE(看 LEAD 表) 可见分数；本人可见自评得分
            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { return false; }

            case RoleConst.ROLE_COMMITTEE -> { return !isLeadTable(t); }

            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION -> {

                // 取当前用户上下文
                Long uid = DataScopeContext.currentUserId();
                return uid == null || !uid.equals(t.getUserId());
            }

            case RoleConst.ROLE_DEPT_LEAD -> {

                // 条件分支
                if (isOwn(t)) {

                    return false;
                }

                // 取当前用户上下文
                Long deptId = DataScopeContext.currentDeptId();
                return deptId == null || !deptId.equals(t.getDeptId());
            }

            default -> { return true; }
        }
    }

    /**
     * 在列表查询上叠加当前用户的可见范围条件：员工仅本人，部门领导限本部门或本人，
     * 委员会限 LEAD 的表或已完成结果，LEAD 过滤掉自评挂起阶段；返回叠加后的查询包装器。
     * @param qw 待叠加条件的查询包装器
     */
    public QueryWrapper<AssessmentTable> scopeOf(QueryWrapper<AssessmentTable> qw) {

        String role = currentRole();

        // 判空处理
        if (role == null) {

            return qw;
        }

        // 按角色或类型分发
        switch (role) {

            case RoleConst.ROLE_LEAD -> {
                // 自评挂起(待人事推送)阶段对领导不可见
                qw.ne("state", AssessmentState.SELF_SUSPENDED.name());
                return qw;
            }

            case RoleConst.ROLE_PERFORMANCE_HR -> { return qw; }

            case RoleConst.ROLE_COMMITTEE -> {
                // 委员会：LEAD 的表（任意状态）+ 全公司已完成
                qw.and(q -> q
                        .in("user_id", leadUserIds())
                        .or(w -> w.eq("state", AssessmentState.FINISHED.name())));
                return qw;
            }

            case RoleConst.ROLE_DEPT_LEAD -> {

                // 取当前用户上下文
                Long deptId = DataScopeContext.currentDeptId();
                // 取当前用户上下文
                Long uid = DataScopeContext.currentUserId();

                qw.and(q -> {

                    q.eq("dept_id", deptId).or(w -> {

                        // 非空才处理
                        if (uid != null) {

                            w.eq("user_id", uid);

                        } else {

                            w.eq("dept_id", -1L); // 恒 false 占位
                        }

                    });
                });
                // 自评挂起(待人事推送)阶段对部门领导不可见（本人除外）
                qw.and(q -> q.ne("state", AssessmentState.SELF_SUSPENDED.name())
                        .or(w -> w.eq("user_id", uid)));
                return qw;
            }

            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION -> {

                // 取当前用户上下文
                Long uid = DataScopeContext.currentUserId();
                if (uid != null) qw.eq("user_id", uid);
                return qw;
            }

            default -> { return qw; }
        }
    }

    /**
     * 返回当前用户在某周期内可见的考核主表 ID 集合（叠加角色可见性条件）。
     * @param periodId 考核周期 ID，可为空表示不限定周期
     */
    public List<Long> visibleTableIds(Long periodId) {

        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        qw.select("id");
        qw = scopeOf(qw);
        if (periodId != null) qw.eq("period_id", periodId);
        // 查询列表
        return tableMapper.selectList(qw).stream().map(AssessmentTable::getId).collect(Collectors.toList());
    }

    // ==================== 辅助 ====================

    private boolean isAssessed(String role) {

        return RoleConst.ASSESSED_ROLES.contains(role);
    }

    private boolean isOwn(AssessmentTable t) {

        // 取当前用户上下文
        Long uid = DataScopeContext.currentUserId();
        return uid != null && uid.equals(t.getUserId());
    }

    private boolean isLeadTable(AssessmentTable t) {

        // 查询单条
        SysUser u = userMapper.selectById(t.getUserId());
        return u != null && RoleConst.ROLE_LEAD.equals(u.getRole());
    }

    private List<Long> leadUserIds() {

        // 查询列表
        return userMapper.selectList(new QueryWrapper<SysUser>()
                        .eq("role", RoleConst.ROLE_LEAD)
                        .eq("status", 1))
                // 集合流处理
                .stream().map(SysUser::getId).collect(Collectors.toList());
    }
}
