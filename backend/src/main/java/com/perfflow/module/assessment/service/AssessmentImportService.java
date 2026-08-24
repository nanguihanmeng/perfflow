package com.perfflow.module.assessment.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.enums.RowCategory;
import com.perfflow.module.assessment.mapper.AssessmentRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * 考核明细导入（仅 HR）。
 *
 * <p>模板与桌面《某公司.xlsx》岗位季度绩效考核表 A1:G16 一致：
 * 第 4 行为表头，第 5-14 行为 10 行数据，列：B=序号、C=指标类别、D=指标名称、
 * E=指标分数、F=工作目标、G=评分标准。按序号(1-10)覆盖写入 assessment_row，幂等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentImportService {

    private final AssessmentRowMapper rowMapper;
    private final AssessmentTableService tableService;

    /** 数据起始行（表头在第 4 行，数据从第 5 行开始） */
    private static final int DATA_START_ROW = 4;
    private static final int DATA_ROW_COUNT = 10;
    /** 导入文件大小上限 5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 导入考核明细到指定主表。
     *
     * @param tableId 主表ID
     * @param bytes   xlsx 文件字节
     */
    @Transactional
    public void importRows(Long tableId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入文件为空");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入文件过大（上限 5MB）");
        }
        AssessmentTable t = tableService.getRequired(tableId);
        if (!"SELF_DRAFTING".equals(t.getState())) {
            throw new BizException(ResultCode.STATE_NOT_ALLOWED, "仅自评中状态可导入考核明细");
        }
        List<AssessmentRow> rows = rowMapper.selectList(
                new QueryWrapper<AssessmentRow>().eq("table_id", tableId).orderByAsc("seq"));
        if (rows.size() != DATA_ROW_COUNT) {
            throw new BizException(ResultCode.BAD_REQUEST, "考核表行数异常，无法导入");
        }

        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            // 从第 5 行开始读取 10 行，列下标与模板一致（0 起）：
            // B=1 序号, C=2 指标类别, D=3 指标名称, E=4 指标分数, F=5 工作目标, G=6 评分标准
            for (int i = 0; i < DATA_ROW_COUNT; i++) {
                int excelRow = DATA_START_ROW + i;
                AssessmentRow r = rows.get(i);

                String indicatorName = reader.readCellValue(3, excelRow) == null ? null
                        : String.valueOf(reader.readCellValue(3, excelRow)).trim();
                String workTarget = reader.readCellValue(5, excelRow) == null ? null
                        : String.valueOf(reader.readCellValue(5, excelRow)).trim();
                String scoreCriteria = reader.readCellValue(6, excelRow) == null ? null
                        : String.valueOf(reader.readCellValue(6, excelRow)).trim();
                BigDecimal baseScore = toBigDecimal(reader.readCellValue(4, excelRow));

                // 序号（B 列）用于校验，与行 seq 对应
                Integer seqFromExcel = toInteger(reader.readCellValue(1, excelRow));

                // 指标名称必填（第 1 行起）；加减分项允许留空
                if (i == 0 && (indicatorName == null || indicatorName.isEmpty())) {
                    throw new BizException(ResultCode.BAD_REQUEST,
                            "第 5 行（序号1）指标名称不能为空，请按模板填写");
                }
                if (seqFromExcel != null && !seqFromExcel.equals(r.getSeq())) {
                    throw new BizException(ResultCode.BAD_REQUEST,
                            "第 " + (excelRow + 1) + " 行序号(" + seqFromExcel + ")与考核表序号("
                                    + r.getSeq() + ")不一致");
                }

                r.setIndicatorName(indicatorName);
                r.setWorkTarget(workTarget);
                r.setScoreCriteria(scoreCriteria);
                if (baseScore != null) {
                    r.setBaseScore(baseScore);
                }
                rowMapper.updateById(r);
            }
            log.info("import rows for table {}: {} rows", tableId, DATA_ROW_COUNT);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object v) {
        if (v == null) return null;
        try {
            return (int) Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
