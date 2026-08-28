package com.perfflow.common.excel;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import com.perfflow.module.deptassessment.entity.DeptKpiRow;
import com.perfflow.module.deptassessment.mapper.DeptKpiRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

/**
 * Excel 数据导入服务（个人考核 / 部门考核）。
 *
 * <p>个人导入：列 = 序号/指标类别/指标名称/指标分数/工作目标/评分标准/完成率/自评得分，按 table_id+seq 覆盖写。
 * <p>部门导入：列 = 行类型/序号/指标名称/目标值/实际完成值/评分标准/得分/权重，按 dept_assessment_id+seq_no 覆盖写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    /** 文件大小上限 5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final AssessmentRowMapper assessmentRowMapper;
    private final DeptKpiRowMapper deptKpiRowMapper;

    /**
     * 导入个人考核数据（按 tableId + seq 覆盖写行）。
     *
     * @param tableId 主表ID
     * @param bytes   xlsx 字节
     */
    @Transactional(rollbackFor = Exception.class)
    public void importPersonal(Long tableId, byte[] bytes) {
        assertFile(bytes);
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            int lastRow = reader.getRowCount();
            for (int row = 1; row < lastRow; row++) {
                Integer seq = toInteger(reader.readCellValue(0, row));
                if (seq == null) {
                    continue;
                }
                AssessmentRow target = assessmentRowMapper.selectOne(new QueryWrapper<AssessmentRow>()
                        .eq("table_id", tableId)
                        .eq("seq", seq)
                        .last("LIMIT 1"));
                if (target == null) {
                    continue;
                }
                target.setIndicatorName(cellStr(reader, 2, row));
                BigDecimal baseScore = toBigDecimal(reader.readCellValue(3, row));
                if (baseScore != null) {
                    target.setBaseScore(baseScore);
                }
                target.setWorkTarget(cellStr(reader, 4, row));
                target.setScoreCriteria(cellStr(reader, 5, row));
                BigDecimal rate = toBigDecimal(reader.readCellValue(6, row));
                if (rate != null) {
                    target.setCompletionRate(rate);
                }
                assessmentRowMapper.updateById(target);
            }
            log.info("个人考核导入完成: tableId={}", tableId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }
    }

    /**
     * 导入部门考核数据（按 assessmentId + seqNo 覆盖写行）。
     *
     * @param assessmentId 部门考核主表ID
     * @param bytes        xlsx 字节
     */
    @Transactional(rollbackFor = Exception.class)
    public void importDept(Long assessmentId, byte[] bytes) {
        assertFile(bytes);
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            int lastRow = reader.getRowCount();
            for (int row = 1; row < lastRow; row++) {
                Integer seqNo = toInteger(reader.readCellValue(1, row));
                if (seqNo == null) {
                    continue;
                }
                String rowType = cellStr(reader, 0, row);
                DeptKpiRow target = deptKpiRowMapper.selectOne(new QueryWrapper<DeptKpiRow>()
                        .eq("dept_assessment_id", assessmentId)
                        .eq("seq_no", seqNo)
                        .last("LIMIT 1"));
                if (target == null) {
                    target = new DeptKpiRow();
                    target.setDeptAssessmentId(assessmentId);
                    target.setSeqNo(seqNo);
                }
                target.setRowType(rowType == null ? "经营业绩" : rowType);
                target.setIndicatorName(cellStr(reader, 2, row));
                target.setTargetValue(cellStr(reader, 3, row));
                target.setActualValue(cellStr(reader, 4, row));
                target.setScoringStandard(cellStr(reader, 5, row));
                target.setScore(toBigDecimal(reader.readCellValue(6, row)));
                target.setWeight(toBigDecimal(reader.readCellValue(7, row)));
                if (target.getId() == null) {
                    deptKpiRowMapper.insert(target);
                } else {
                    deptKpiRowMapper.updateById(target);
                }
            }
            log.info("部门考核导入完成: assessmentId={}", assessmentId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }
    }

    private void assertFile(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入文件为空");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入文件过大（上限 5MB）");
        }
    }

    private String cellStr(ExcelReader reader, int col, int row) {
        Object v = reader.readCellValue(col, row);
        return v == null ? null : String.valueOf(v).trim();
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
