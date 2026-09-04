package com.perfflow.common.excel;

import cn.hutool.poi.excel.ExcelReader;
import com.perfflow.common.api.ResultCode;
import com.perfflow.common.exception.BizException;

import java.math.BigDecimal;

/**
 * Excel 导入通用解析工具：单元格取值与上传文件基础校验。
 * 供个人/部门/周期各导入服务复用，避免重复实现。
 */
public final class ExcelReadUtil {

    /** 上传文件大小上限。 */
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private ExcelReadUtil() {
    }

    /** 校验上传文件非空且未超限。 */
    public static void assertFile(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入文件为空");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "导入文件过大（上限 5MB）");
        }
    }

    /** 读取单元格字符串并去除首尾空白，空单元格返回 null。 */
    public static String cellStr(ExcelReader reader, int col, int row) {
        Object v = reader.readCellValue(col, row);
        return v == null ? null : String.valueOf(v).trim();
    }

    /** 读取单元格数值，为空或无法解析时返回 null（BigDecimal 以字符串构造避免精度丢失）。 */
    public static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 读取单元格整数（小数向下取整），为空或无法解析时返回 null。 */
    public static Integer toInteger(Object v) {
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
