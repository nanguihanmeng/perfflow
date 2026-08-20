package com.perfflow.module.assessment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.dto.RowReq;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.AssessmentState;
import com.perfflow.module.assessment.enums.RowCategory;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.security.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 考核行服务。
 *
 * <p>自评得分由后端按「指标分数 × 完成率%」自动计算，任何人不可直接写得分；
 * 行结果（考核结果）只与总分相关，行级不落库。
 */
@Service
@RequiredArgsConstructor
public class AssessmentRowService {

    private final AssessmentRowMapper rowMapper;
    private final AssessmentTableService tableService;
    private final AssessmentPermissionService perm;
    private final AssessmentCalcService calcService;

    /**
     * 更新考核行。
     *
     * <p>可写字段按角色区分：
     * <ul>
     *   <li>EMP（SELF_DRAFTING，本人）：完成率 completionRate</li>
     *   <li>DEPT_LEAD（DEPT_REVIEW，本部门）：完成率 completionRate（代替原调分）</li>
     * </ul>
     * 每次更新后即时重算整表自评总分。
     *
     * @param tableId 主表ID
     * @param rowId   行ID
     * @param req     更新请求（仅 completionRate）
     */
    @Transactional
    public void update(Long tableId, Long rowId, RowReq req) {
        AssessmentTable t = tableService.getRequired(tableId);
        AssessmentRow r = getRow(rowId);
        if (!perm.canEditRow(t, r)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (req.getCompletionRate() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "完成率不能为空");
        }
        r.setCompletionRate(req.getCompletionRate());
        // 员工填写主表信息栏：岗位
        if (req.getPosition() != null && perm.isEmp() && t.getUserId().equals(DataScopeContext.currentUserId())) {
            t.setPosition(req.getPosition());
            tableService.updateTablePosition(t);
        }
        rowMapper.updateById(r);
        calcService.recalc(t);
    }

    /**
     * 岗位信息更新（员工在考核表信息栏填写）。
     *
     * @param tableId 主表ID
     * @param position 岗位
     */
    @Transactional
    public void updatePosition(Long tableId, String position) {
        AssessmentTable t = tableService.getRequired(tableId);
        if (!perm.isEmp() || !t.getUserId().equals(DataScopeContext.currentUserId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!AssessmentState.SELF_DRAFTING.name().equals(t.getState())) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED);
        }
        t.setPosition(position);
        tableService.updateTablePosition(t);
    }

    public AssessmentRow getRow(Long rowId) {
        AssessmentRow r = rowMapper.selectById(rowId);
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        return r;
    }

    /** 强制 BONUS 行不允许编辑：表状态机已经强制，调用被 canEditRow 拒绝 */
    public boolean isBonusRow(AssessmentRow r) {
        return RowCategory.BONUS.name().equals(r.getCategory());
    }
}
