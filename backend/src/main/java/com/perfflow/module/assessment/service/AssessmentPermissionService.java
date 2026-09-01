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
// 评估数据权限 + 字段脱敏判定。
@Service
@RequiredArgsConstructor
public class AssessmentPermissionService {

    private static final List<String> ASSESSED_ROLES = List.of(
            RoleConst.ROLE_EMP, RoleConst.ROLE_DEPT_LEAD, RoleConst.ROLE_LEAD,
            RoleConst.ROLE_DEPT_STAFF, RoleConst.ROLE_OPERATION, RoleConst.ROLE_COMMITTEE);
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

    // 执行业务处理
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

    // 执行业务处理
    public boolean canEditRow(AssessmentTable t, AssessmentRow r) {

        String role = currentRole();
        if (role == null) return false;
        AssessmentState cur = AssessmentState.valueOf(t.getState());
        boolean isPlanOrOpen = r.getCategory() != null && !"BONUS".equals(r.getCategory());

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

    // 执行业务处理
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

    // 在主表 list 查询上叠加可见性条件。
     // 返回 true 时调用方按当前用户筛选；false 表示已是全量（如 LEAD/HR）。

    // 执行业务处理
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

    // 执行业务处理
    public List<Long> visibleTableIds(Long periodId) {

        QueryWrapper<AssessmentTable> qw = new QueryWrapper<>();
        qw.select("id");
        qw = scopeOf(qw);
        if (periodId != null) qw.eq("period_id", periodId);
        // 查询列表
        return tableMapper.selectList(qw).stream().map(AssessmentTable::getId).collect(Collectors.toList());
    }

    // 执行 visibleStates。

    // 执行业务处理
    public Set<String> visibleStates() {
        // 当前所有状态都可见，仅用于过滤器
        return Collections.emptySet();
    }

    // ==================== 辅助 ====================

    private boolean isAssessed(String role) {

        return ASSESSED_ROLES.contains(role);
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
