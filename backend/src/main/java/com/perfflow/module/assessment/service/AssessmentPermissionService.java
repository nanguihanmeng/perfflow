package com.perfflow.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.mapper.AssessmentTableMapper;
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
 *   <li>EMP：仅可访问自己的表；调整得分列与最终分/等级等敏感列对本人也脱敏</li>
 *   <li>DEPT_LEAD：可读本部门所有表；同员工对同事分数同样脱敏</li>
 *   <li>LEAD / HR：可见全公司，不脱敏</li>
 *   <li>ADMIN：见不到业务接口（已在 WebMvcConfig 拦截）</li>
 * </ul>
 *
 * <p>同时实现行级可见性：查询时附加 {@code IN (...)} 条件。
 */
@Service
@RequiredArgsConstructor
public class AssessmentPermissionService {

    private final AssessmentTableMapper tableMapper;

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
        switch (role) {
            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { /* all */ }
            case RoleConst.ROLE_DEPT_LEAD -> {
                Long uid = DataScopeContext.currentDeptId();
                if (uid == null || !uid.equals(t.getDeptId())) {
                    throw new BizException(ResultCode.FORBIDDEN);
                }
            }
            case RoleConst.ROLE_EMP -> {
                Long uid = DataScopeContext.currentUserId();
                if (uid == null || !uid.equals(t.getUserId())) {
                    throw new BizException(ResultCode.FORBIDDEN);
                }
            }
            default -> throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    /** 当前用户能否改某行（员工填完成率；部门领导在审核阶段改完成率） */
    public boolean canEditRow(AssessmentTable t, AssessmentRow r) {
        String role = currentRole();
        if (role == null) return false;
        AssessmentState cur = AssessmentState.valueOf(t.getState());
        return switch (role) {
            case RoleConst.ROLE_EMP -> {
                boolean isMine = DataScopeContext.currentUserId() != null
                        && DataScopeContext.currentUserId().equals(t.getUserId());
                boolean isPlanOrOpen = r.getCategory() != null
                        && !"BONUS".equals(r.getCategory());
                yield isMine && cur == AssessmentState.SELF_DRAFTING && isPlanOrOpen;
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
                boolean sameDept = DataScopeContext.currentDeptId() != null
                        && DataScopeContext.currentDeptId().equals(t.getDeptId());
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
            // 员工本人 / 本部门领导 / LEAD / HR 可见自评得分
            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { return false; }
            case RoleConst.ROLE_EMP -> {
                Long uid = DataScopeContext.currentUserId();
                return uid == null || !uid.equals(t.getUserId());
            }
            case RoleConst.ROLE_DEPT_LEAD -> {
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
            case RoleConst.ROLE_LEAD, RoleConst.ROLE_PERFORMANCE_HR -> { return qw; }
            case RoleConst.ROLE_DEPT_LEAD -> {
                Long deptId = DataScopeContext.currentDeptId();
                if (deptId != null) qw.eq("dept_id", deptId);
                return qw;
            }
            case RoleConst.ROLE_EMP -> {
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
}
