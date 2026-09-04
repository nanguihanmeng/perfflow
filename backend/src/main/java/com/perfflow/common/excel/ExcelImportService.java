package com.perfflow.common.excel;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.module.deptassessment.entity.DeptAssessment;
import com.perfflow.module.deptassessment.entity.DeptKpiRow;
import com.perfflow.module.deptassessment.enums.DeptAssessmentState;
import com.perfflow.module.deptassessment.mapper.DeptAssessmentMapper;
import com.perfflow.module.deptassessment.mapper.DeptKpiRowMapper;
import com.perfflow.module.deptassessment.service.DeptAssessmentService;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.period.mapper.AssessmentPeriodMapper;
import com.perfflow.module.system.entity.SysDepartment;
import com.perfflow.module.system.mapper.SysDepartmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//
 // Excel 数据导入服务（个人考核 / 部门考核）。
 //

 // 得分由系统按完成率自动计算，实际完成值由绩效专员在页面填报，均不在 HR 导入中维护。
 //
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final AssessmentRowMapper assessmentRowMapper;
    private final DeptKpiRowMapper deptKpiRowMapper;
    private final DeptAssessmentMapper deptAssessmentMapper;
    private final SysDepartmentMapper deptMapper;
    private final AssessmentPeriodMapper periodMapper;

    //
     // 导入个人考核数据，按 tableId + seq 覆盖写行。
     //

     //
    @Transactional(rollbackFor = Exception.class)
    public void importPersonal(Long tableId, byte[] bytes) {
        // 校验文件合法性
        ExcelReadUtil.assertFile(bytes);
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            int lastRow = reader.getRowCount();
            for (int row = 1; row < lastRow; row++) {
                Integer seq = ExcelReadUtil.toInteger(reader.readCellValue(0, row));
                // 判空处理
                if (seq == null) {
                    continue;
                }
                // 按 主表ID+序号 定位目标行
                AssessmentRow target = assessmentRowMapper.selectOne(new QueryWrapper<AssessmentRow>()
                        .eq("table_id", tableId)
                        .eq("seq", seq)
                        .last("LIMIT 1"));
                // 判空处理
                if (target == null) {
                    continue;
                }
                // 覆盖写指标字段
                target.setIndicatorName(ExcelReadUtil.cellStr(reader, 2, row));
                BigDecimal baseScore = ExcelReadUtil.toBigDecimal(reader.readCellValue(3, row));
                // 非空才处理
                if (baseScore != null) {
                    target.setBaseScore(baseScore);
                }
                target.setWorkTarget(ExcelReadUtil.cellStr(reader, 4, row));
                target.setScoreCriteria(ExcelReadUtil.cellStr(reader, 5, row));
                BigDecimal rate = ExcelReadUtil.toBigDecimal(reader.readCellValue(6, row));
                // 非空才处理
                if (rate != null) {
                    target.setCompletionRate(rate);
                }
                // 更新记录
                assessmentRowMapper.updateById(target);
            }
            log.info("个人考核导入完成: tableId={}", tableId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }
    }

    //
     // 导入单部门考核指标，按 assessmentId + seqNo 覆盖写行。
     //

     //

     //
    @Transactional(rollbackFor = Exception.class)
    public void importDept(Long assessmentId, byte[] bytes) {
        ExcelReadUtil.assertFile(bytes);
        // 校验考核存在且处于自评中
        DeptAssessment assessment = deptAssessmentMapper.selectById(assessmentId);
        // 判空处理
        if (assessment == null) {
            // 校验失败抛异常
            throw new BizException(ResultCode.DEPT_ASSESS_NOT_FOUND);
        }
        // 状态判断
        if (!Integer.valueOf(DeptAssessmentState.SELF_FILLING.getCode()).equals(assessment.getStatus())) {
            // 校验失败抛异常
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "仅填报中的部门考核可导入指标");
        }
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            int lastRow = reader.getRowCount();
            // 跳过模板顶部的标题与格式说明行，定位到表头行
            int dataStart = findDataStartRow(reader, lastRow);
            for (int row = dataStart; row < lastRow; row++) {
                Integer seqNo = ExcelReadUtil.toInteger(reader.readCellValue(2, row));
                // 判空处理
                if (seqNo == null) {
                    continue;
                }
                String rowType = ExcelReadUtil.cellStr(reader, 1, row);
                // 按 考核主表ID+序号 定位 KPI 行
                DeptKpiRow target = deptKpiRowMapper.selectOne(new QueryWrapper<DeptKpiRow>()
                        .eq("dept_assessment_id", assessmentId)
                        .eq("seq_no", seqNo)
                        .last("LIMIT 1"));
                // 判空处理
                if (target == null) {
                    target = new DeptKpiRow();
                    target.setDeptAssessmentId(assessmentId);
                    target.setSeqNo(seqNo);
                }
                // 仅写入指标列，实际完成值与得分不在此维护
                target.setRowType(rowType == null ? DeptAssessmentService.ROW_TYPE_KPI : rowType);
                target.setIndicatorName(ExcelReadUtil.cellStr(reader, 3, row));
                target.setTargetValue(ExcelReadUtil.cellStr(reader, 4, row));
                target.setScoringStandard(ExcelReadUtil.cellStr(reader, 5, row));
                target.setWeight(ExcelReadUtil.toBigDecimal(reader.readCellValue(6, row)));
                // 判空处理
                if (target.getId() == null) {
                    // 写入记录
                    deptKpiRowMapper.insert(target);
                } else {
                    // 更新记录
                    deptKpiRowMapper.updateById(target);
                }
            }
            log.info("部门考核指标导入完成: assessmentId={}", assessmentId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }
    }

    //
     // 按周期批量导入部门考核指标。
     //

     //

     //
    @Transactional(rollbackFor = Exception.class)
    public void importDeptBatch(Long periodId, byte[] bytes) {
        ExcelReadUtil.assertFile(bytes);
        // 校验周期存在
        AssessmentPeriod period = periodMapper.selectById(periodId);
        // 判空处理
        if (period == null) {
            // 校验失败抛异常
            throw new BizException(ResultCode.NOT_FOUND, "周期不存在");
        }
        // 建立 部门ID->部门名 映射
        Map<Long, String> deptNames = new HashMap<>();
        // 查询列表
        for (SysDepartment d : deptMapper.selectList(new QueryWrapper<SysDepartment>())) {
            deptNames.put(d.getId(), d.getName());
        }
        // 建立 部门名->考核主表 映射，供按名匹配
        List<DeptAssessment> assessments = deptAssessmentMapper.selectList(
                new QueryWrapper<DeptAssessment>().eq("period_id", periodId));
        Map<String, DeptAssessment> byDeptName = new HashMap<>();
        for (DeptAssessment a : assessments) {
            String name = deptNames.get(a.getDeptId());
            // 非空才处理
            if (name != null) {
                byDeptName.put(name, a);
            }
        }
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            int lastRow = reader.getRowCount();
            // 跳过模板顶部的标题与格式说明行，定位到表头行
            int dataStart = findDataStartRow(reader, lastRow);
            for (int row = dataStart; row < lastRow; row++) {
                String deptName = ExcelReadUtil.cellStr(reader, 0, row);
                Integer seqNo = ExcelReadUtil.toInteger(reader.readCellValue(2, row));
                // 判空处理
                if (deptName == null || seqNo == null) {
                    continue;
                }
                // 按部门名与序号定位考核主表
                DeptAssessment assessment = byDeptName.get(deptName.trim());
                // 判空处理
                if (assessment == null) {
                    continue;
                }
                // 仅自评中的考核可导入
                if (!Integer.valueOf(DeptAssessmentState.SELF_FILLING.getCode()).equals(assessment.getStatus())) {
                    continue;
                }
                // 查询单条
                DeptKpiRow target = deptKpiRowMapper.selectOne(new QueryWrapper<DeptKpiRow>()
                        .eq("dept_assessment_id", assessment.getId())
                        .eq("seq_no", seqNo)
                        .last("LIMIT 1"));
                // 判空处理
                if (target == null) {
                    target = new DeptKpiRow();
                    target.setDeptAssessmentId(assessment.getId());
                    target.setSeqNo(seqNo);
                }
                // 仅写入指标列
                target.setRowType(ExcelReadUtil.cellStr(reader, 1, row) == null
                        ? DeptAssessmentService.ROW_TYPE_KPI : ExcelReadUtil.cellStr(reader, 1, row));
                target.setIndicatorName(ExcelReadUtil.cellStr(reader, 3, row));
                target.setTargetValue(ExcelReadUtil.cellStr(reader, 4, row));
                target.setScoringStandard(ExcelReadUtil.cellStr(reader, 5, row));
                target.setWeight(ExcelReadUtil.toBigDecimal(reader.readCellValue(6, row)));
                // 判空处理
                if (target.getId() == null) {
                    // 写入记录
                    deptKpiRowMapper.insert(target);
                } else {
                    // 更新记录
                    deptKpiRowMapper.updateById(target);
                }
            }
            log.info("部门考核指标批量导入完成: periodId={}", periodId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "批量导入失败，请检查文件格式: " + e.getMessage());
        }
    }

    // 定位数据起始行：跳过模板顶部的标题与格式说明，返回表头行号。
     // 表头行包含「部门」「行类型」「序号」等列名；找不到时回退到第 1 行。
    private int findDataStartRow(ExcelReader reader, int lastRow) {

        // 从第 0 行开始找表头
        for (int row = 0; row < lastRow; row++) {

            String dept = ExcelReadUtil.cellStr(reader, 0, row);
            String seq = ExcelReadUtil.cellStr(reader, 2, row);
            // 同时命中「部门」列与「序号」列即视为表头行
            if ("部门".equals(dept) && "序号".equals(seq)) {

                // 表头行的下一行是数据起始行
                return row + 1;
            }
        }

        // 未找到表头，按旧格式回退到第 1 行开始
        return 1;
    }

}
