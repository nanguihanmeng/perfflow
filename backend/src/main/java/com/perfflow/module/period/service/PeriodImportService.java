package com.perfflow.module.period.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.exception.BizException;
import com.perfflow.module.assessment.entity.AssessmentRow;
import com.perfflow.module.assessment.entity.AssessmentTable;
import com.perfflow.module.assessment.service.AssessmentTableService;
import com.perfflow.module.period.entity.AssessmentPeriod;
import com.perfflow.module.system.entity.SysUser;
import com.perfflow.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 周期级考核明细导入（绩效考核管理员发布周期时用）。
 *
 * <p>Excel 格式约定（一个文件可含多个员工，每员工占 10 行）：
 * <ul>
 *   <li>A 列：员工登录名（该员工 10 行的第 1 行标注，后续 9 行留空）</li>
 *   <li>B 列：序号 1-10</li>
 *   <li>C 列：指标类别（PLAN/OPEN/BONUS）</li>
 *   <li>D 列：指标名称</li>
 *   <li>E 列：指标分数</li>
 *   <li>F 列：工作目标</li>
 *   <li>G 列：评分标准</li>
 * </ul>
 * 导入后：按登录名匹配 EMP 员工 → 生成主表 + 10 行模板 → 写入考核明细。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodImportService {

    private static final int ROWS_PER_EMPLOYEE = 10;

    private final SysUserMapper userMapper;
    private final AssessmentTableService tableService;

    /**
     * 导入周期考核明细，返回参与考核的员工 ID 列表。
     *
     * @param period 周期（需已存在，未开启状态）
     * @param bytes  xlsx 文件字节
     * @return 参与考核的员工 ID 列表
     */
    @Transactional
    public List<Long> importPeriod(AssessmentPeriod period, byte[] bytes) {
        List<Long> userIds = new ArrayList<>();
        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {
            int lastRow = reader.getRowCount();
            String currentUsername = null;
            List<Integer> blockRows = new ArrayList<>();

            for (int row = 0; row < lastRow; row++) {
                Object usernameCell = reader.readCellValue(0, row);
                boolean isBlockStart = usernameCell != null
                        && !String.valueOf(usernameCell).trim().isEmpty();

                if (isBlockStart && !blockRows.isEmpty()) {
                    // 上一个员工块结束（新员工出现）
                    userIds.add(processEmployee(period, currentUsername, reader, blockRows));
                    currentUsername = String.valueOf(usernameCell).trim();
                    blockRows = new ArrayList<>();
                } else if (isBlockStart) {
                    currentUsername = String.valueOf(usernameCell).trim();
                    blockRows = new ArrayList<>();
                } else if (blockRows.size() == ROWS_PER_EMPLOYEE) {
                    // 当前块已满 10 行且本行无新员工名 → 属于下一个员工（该员工名缺失则报错）
                    if (currentUsername != null) {
                        userIds.add(processEmployee(period, currentUsername, reader, blockRows));
                    }
                    currentUsername = null;
                    blockRows = new ArrayList<>();
                }
                blockRows.add(row);
            }
            // 最后一个员工块
            if (currentUsername != null && !blockRows.isEmpty()) {
                userIds.add(processEmployee(period, currentUsername, reader, blockRows));
            }
            if (userIds.isEmpty()) {
                throw new BizException(ResultCode.BAD_REQUEST, "Excel 中未解析到任何员工考核明细");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }
        return userIds;
    }

    /** 处理单个员工的 10 行明细 */
    private Long processEmployee(AssessmentPeriod period, String username,
                                 ExcelReader reader, List<Integer> rows) {
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username)
                .eq("role", RoleConst.ROLE_EMP)
                .eq("status", 1));
        if (user == null || user.getDeptId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "Excel 中的员工「" + username + "」不存在或不可参与考核");
        }
        if (rows.size() != ROWS_PER_EMPLOYEE) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "员工「" + username + "」的考核明细行数异常（应为 10 行）");
        }

        // 生成主表 + 10 行模板
        List<Long> single = List.of(user.getId());
        tableService.initForPeriod(period, single);
        AssessmentTable table = tableService.findByPeriodAndUser(period.getId(), user.getId());
        if (table == null) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "员工「" + username + "」考核表生成失败");
        }

        // 写入明细（列：B=1序号 C=2类别 D=3指标名 E=4分数 F=5目标 G=6标准）
        List<AssessmentRow> tableRows = tableService.listRows(table.getId());
        for (int i = 0; i < ROWS_PER_EMPLOYEE; i++) {
            int excelRow = rows.get(i);
            AssessmentRow r = tableRows.get(i);
            r.setIndicatorName(cellStr(reader, 3, excelRow));
            r.setWorkTarget(cellStr(reader, 5, excelRow));
            r.setScoreCriteria(cellStr(reader, 6, excelRow));
            BigDecimal baseScore = cellBig(reader, 4, excelRow);
            if (baseScore != null) {
                r.setBaseScore(baseScore);
            }
            tableService.updateRow(r);
        }
        return user.getId();
    }

    private String cellStr(ExcelReader reader, int col, int row) {
        Object v = reader.readCellValue(col, row);
        return v == null ? null : String.valueOf(v).trim();
    }

    private BigDecimal cellBig(ExcelReader reader, int col, int row) {
        Object v = reader.readCellValue(col, row);
        if (v == null) return null;
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
