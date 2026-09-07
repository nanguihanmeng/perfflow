package com.perfflow.module.period.service;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.constant.RoleConst;
import com.perfflow.common.excel.ExcelReadUtil;
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
 * 周期级考核明细导入（绩效考核管理员发布周期时使用）：按登录名匹配 EMP 员工，
 * 先为员工生成考核主表与 10 行模板，再写入 Excel 明细。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodImportService {

    private static final int ROWS_PER_EMPLOYEE = 10;
    private final SysUserMapper userMapper;
    private final AssessmentTableService tableService;
    /**
     * 导入周期考核明细：按"员工名所在行 + 后续 10 行明细"划分数据块，
     * 每个块匹配 EMP 员工并写入其考核表，返回参与考核的员工 ID 列表。
     * @param period 目标考核周期
     * @param bytes Excel 文件字节流
     */
    @Transactional
    public List<Long> importPeriod(AssessmentPeriod period, byte[] bytes) {

        // 构建集合容器
        List<Long> userIds = new ArrayList<>();

        try (ExcelReader reader = ExcelUtil.getReader(new ByteArrayInputStream(bytes))) {

            int lastRow = reader.getRowCount();
            String currentUsername = null;
            // 构建集合容器
            List<Integer> blockRows = new ArrayList<>();

            for (int row = 0; row < lastRow; row++) {

                String usernameCell = ExcelReadUtil.cellStr(reader, 0, row);
                boolean isBlockStart = usernameCell != null && !usernameCell.isEmpty();

                // 条件分支
                if (isBlockStart && !blockRows.isEmpty()) {
                    // 上一个员工块结束（新员工出现）
                    userIds.add(processEmployee(period, currentUsername, reader, blockRows));
                    currentUsername = usernameCell;
                    blockRows = new ArrayList<>();

                } else if (isBlockStart) {

                    currentUsername = usernameCell;
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

            // 条件分支
            if (userIds.isEmpty()) {

                // 校验失败抛异常
                throw new BizException(ResultCode.BAD_REQUEST, "Excel 中未解析到任何员工考核明细");
            }

            log.info("周期导入考核明细: periodId={}, 员工数={}, userIds={}",
                    period.getId(), userIds.size(), userIds);

        } catch (BizException e) {

            throw e;

        } catch (Exception e) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST, "导入失败，请检查文件格式: " + e.getMessage());
        }

        return userIds;
    }

    private Long processEmployee(AssessmentPeriod period, String username,

                                 ExcelReader reader, List<Integer> rows) {

        // 查询单条
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username)
                .eq("role", RoleConst.ROLE_EMP)
                .eq("status", 1));

        // 判空处理
        if (user == null || user.getDeptId() == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "Excel 中的员工「" + username + "」不存在或不可参与考核");
        }

        // 值比较
        if (rows.size() != ROWS_PER_EMPLOYEE) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "员工「" + username + "」的考核明细行数异常（应为 10 行）");
        }

        // 生成主表 + 10 行模板
        List<Long> single = List.of(user.getId());
        // 调用业务服务
        tableService.initForPeriod(period, single);
        // 调用业务服务
        AssessmentTable table = tableService.findByPeriodAndUser(period.getId(), user.getId());

        // 判空处理
        if (table == null) {

            // 校验失败抛异常
            throw new BizException(ResultCode.BAD_REQUEST,
                    "员工「" + username + "」考核表生成失败");
        }

        // 写入明细（列：B=1序号 C=2类别 D=3指标名 E=4分数 F=5目标 G=6标准）
        List<AssessmentRow> tableRows = tableService.listRows(table.getId());

        for (int i = 0; i < ROWS_PER_EMPLOYEE; i++) {

            int excelRow = rows.get(i);
            AssessmentRow r = tableRows.get(i);
            r.setIndicatorName(ExcelReadUtil.cellStr(reader, 3, excelRow));
            r.setWorkTarget(ExcelReadUtil.cellStr(reader, 5, excelRow));
            r.setScoreCriteria(ExcelReadUtil.cellStr(reader, 6, excelRow));
            BigDecimal baseScore = ExcelReadUtil.toBigDecimal(reader.readCellValue(4, excelRow));

            // 非空才处理
            if (baseScore != null) {

                r.setBaseScore(baseScore);
            }

            // 调用业务服务
            tableService.updateRow(r);
        }

        return user.getId();
    }

}
