package com.perfflow.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评估数据权限 + 字段脱敏判定。
 *
 * <p>原则：
 * <ul>
 *   <li>被考核人（EMP/DEPT_LEAD/LEAD/DEPT_STAFF/OPERATION/COMMITTEE）：可访问自己的表</li>
 *   <li>DEPT_LEAD：可读本部门所有表 + 本人；对同事分数脱敏</li>
 *   <li>LEAD：可见全公司非挂起 + 本人；COMMITTEE：可见 LEAD 的表（评分）+ 全公司结果</li>
 *   <li>HR：全量；ADMIN：见不到业务接口（已在 WebMvcConfig 拦截）</li>
 * </ul>
 *
 * <p>同时实现行级可见性：查询时附加 {@code IN (...)} 条件。
 */
@Service
@RequiredArgsConstructor
public class AssessmentPermissionService {

    /** 参与个人考核的被考核人角色（排除 ADMIN / HR） */
    private static final List<String> ASSESSED_ROLES = List.of(
            RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_LEAD, RoleConst.ROLE_LEAD,
            RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION, RoleConst.ROLE_COMMITTEE);

    private final AssessmentTableMapper tableMapper;
    private final SysUserMapper userMapper;

    /** 当前角色 */
    public String currentRole() {
        return DataScopeContext.current().getPrimaryRole();
    }

    public boolean isEmp()     { return RoleConst.ROLE_EMP.equals(currentRole()); }
    public boolean isDeptLead(){ return RoleConst.ROLE_DEPT_LEAD.equals(currentRole()); }
    public boolean isLead()    { return RoleConst.ROLE_LEAD.equals(currentRole()); }
    public boolean isHr()      { return RoleConst.ROLE_PERFORMANCE_HR.equals(currentRole()); }

    /** 当前用户是否能查看指定行主表 */
    public void ensureVisible(AssessmentTable t) {
        String role = currentRole();
        if (role == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 自评挂起(待人事推送)阶段：仅 HR 与本人可见，其余角色不可见
        if (AssessmentState.SELF_SUSPENDED.name().equals(t.getState())) {
            if (RoleConst.ROLE_PERFORMANCE_HR.equals(role)) {
                return;
            }
            if (isAssessed(role)) {
                Long uid = DataScopeContext.currentUserId();
                if (uid != null && uid.equals(t.getUserId())) {
                    return;
                }
            }
            throw new BizException(ResultCode.FORBIDDEN);
        }
        switch (role) {
            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { /* all */ }
            case RoleConst.ROLE_COMMITTEE -> {
                // 委员会可见 LEAD 的表（评分对象）+ 全公司已完成结果
                if (isLeadTable(t)) {
                    return;
                }
                if (AssessmentState.FINISHED.name().equals(t.getState())) {
                    return;
                }
                throw new BizException(ResultCode.FORBIDDEN);
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                Long deptId = DataScopeContext.currentDeptId();
                boolean own = isOwn(t);
                if (deptId != null && deptId.equals(t.getDeptId())) {
                    return;
                }
                if (own) {
                    return;
                }
                throw new BizException(ResultCode.FORBIDDEN);
            }
            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION -> {
                if (isOwn(t)) {
                    return;
                }
                throw new BizException(ResultCode.FORBIDDEN);
            }
            default -> throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /** 当前用户能否改某行（员工填完成率；部门领导可改自评得分含加减分项） */
    public boolean canEditRow(AssessmentTable t, AssessmentRow r) {
        String role = currentRole();
        if (role == null) return false;
        AssessmentState cur = AssessmentState.valueOf(t.getState());
        boolean isPlanOrOpen = r.getCategory() != null && !"BONUS".equals(r.getCategory());
        return switch (role) {
            case RoleConst.ROLE_EMP -> {
                boolean isMine = DataScopeContext.currentUserId() != null
                        && DataScopeContext.currentUserId().equals(t.getUserId());
                // 员工不可填加减分项
                yield isMine && cur == AssessmentState.SELF_DRAFTING && isPlanOrOpen;
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                boolean sameDept = DataScopeContext.currentDeptId() != null
                        && DataScopeContext.currentDeptId().equals(t.getDeptId());
                // 部门领导可编辑全部行（含加减分项）
                yield sameDept && cur == AssessmentState.DEPT_REVIEW;
            }
            default -> false;
        };
    }

    /** 当前用户是否应对此表中的行做脱敏（分数相关列隐藏） */
    public boolean isRowMasked(AssessmentTable t) {
        String role = currentRole();
        if (role == null) return true;
        switch (role) {
            // LEAD / HR / COMMITTEE(看 LEAD 表) 可见分数；本人可见自评得分
            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { return false; }
            case RoleConst.ROLE_COMMITTEE -> { return !isLeadTable(t); }
            case RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION -> {
                Long uid = DataScopeContext.currentUserId();
                return uid == null || !uid.equals(t.getUserId());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                if (isOwn(t)) {
                    return false;
                }
                Long deptId = DataScopeContext.currentDeptId();
                return deptId == null || !deptId.equals(t.getDeptId());
            }
            default -> { return true; }
        }
    }

    /**
     * 在主表 list 查询上叠加可见性条件。
     *  返回 true 时调用方按当前用户筛选；false 表示已是全量（如 LEAD/HR）。
     */
    public QueryWrapper<AssessmentTable> scopeOf(QueryWrapper<AssessmentTable> qw) {
        String role = currentRole();
        if (role == null) {
            return qw;
        }
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
                Long deptId = DataScopeContext.currentDeptId();
                Long uid = DataScopeContext.currentUserId();
                qw.and(q -> {
                    q.eq("dept_id", deptId).or(w -> {
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
                Long uid = DataScopeContext.currentUserId();
                if (uid != null) qw.eq("user_id", uid);
                return qw;
            }
            default -> { return qw; }
        }
    }

    /** 仅返回当前用户可见的 id 集合（用于 secondary filter） */
    public List<Long> visibleTableIds(Long periodId) {
        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        qw.select("id");
        qw = scopeOf(qw);
        if (periodId != null) qw.eq("period_id", periodId);
        return tableMapper.selectList(qw).stream().map(AssessmentTable::getId).collect(Collectors.toList());
    }

    public Set<String> visibleStates() {
        // 当前所有状态都可见，仅用于过滤器
        return Collections.emptySet();
    }

    // ==================== 辅助 ====================

    /** 是否被考核人角色（EMP/DEPT_LEAD/LEAD/DEPT_STAFF/OPERATION/COMMITTEE） */
    private boolean isAssessed(String role) {
        return ASSESSED_ROLES.contains(role);
    }

    /** 该表是否属于当前登录用户 */
    private boolean isOwn(AssessmentTable t) {
        Long uid = DataScopeContext.currentUserId();
        return uid != null && uid.equals(t.getUserId());
    }

    /** 该表是否为公司领导（LEAD 角色）的考核表 */
    private boolean isLeadTable(AssessmentTable t) {
        SysUser u = userMapper.selectById(t.getUserId());
        return u != null && RoleConst.ROLE_LEAD.equals(u.getRole());
    }

    /** 全部 LEAD 角色用户 ID 集合 */
    private List<Long> leadUserIds() {
        return userMapper.selectList(new QueryWrapper<SysUser>()
                        .eq("role", RoleConst.ROLE_LEAD)
                        .eq("status", 1))
                .stream().map(SysUser::getId).collect(Collectors.toList());
    }
}
