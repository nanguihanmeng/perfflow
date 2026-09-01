package com.perfflow.common.excel;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Web 风格 Excel 导出通用样式工具。

public final class ExportStyleUtil {

    private static final Color MODULE_TITLE_BG = new Color(0x1F, 0x4E, 0x79);
    private static final Color HEADER_BG = new Color(0xDD, 0xEB, 0xF7);
    private static final Color BAND_COLOR = new Color(0x2E, 0x75, 0xB6);
    private static final int BAND_COLUMN_WIDTH = 2;
    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private final Map<String, CellStyle> styleCache = new HashMap<>(8);
    // writer
    private final ExcelWriter writer;
    // workbook
    private final Workbook workbook;
    // sheet
    private final Sheet sheet;
    // 构造器，初始化工作簿与工作表。

    public ExportStyleUtil(ExcelWriter writer) {
        this.writer = writer;
        this.workbook = writer.getWorkbook();
        this.sheet = writer.getSheet();
        this.sheet.setDisplayGridlines(false);
    }

    // 在指定行写入一级模块标题并合并单元格。

    public void writeModuleTitle(int row, int fromCol, int toCol, String text) {

        writer.merge(row, row, fromCol, toCol, text, false);
        CellStyle style = style("moduleTitle");

        for (int col = fromCol; col <= toCol; col++) {

            sheet.getRow(row).getCell(col).setCellStyle(style);
        }
    }

    // 在指定行写入二级表头。

    public void writeHeaderRow(int row, Object[] texts) {

        for (int col = 0; col < texts.length; col++) {

            Cell cell = getOrCreateCell(row, col);
            cell.setCellValue(safeString(texts[col]));
            cell.setCellStyle(style("header"));
        }
    }

    public void writeDataRow(int row, Object[] texts) {

        for (int col = 0; col < texts.length; col++) {

            Cell cell = getOrCreateCell(row, col);
            setCellValue(cell, texts[col]);
            cell.setCellStyle(style("data"));
        }
    }

    // 在指定行写入普通文本单元格。

    public void writeText(int row, int col, String text) {

        Cell cell = getOrCreateCell(row, col);
        cell.setCellValue(safeString(text));
        cell.setCellStyle(style("data"));
    }

    // 绘制最左侧装饰条（极窄深蓝列），支持多行连续。

    public void paintBand(int fromRow, int toRow, int col) {

        sheet.setColumnWidth(col, BAND_COLUMN_WIDTH * 256);
        CellStyle bandStyle = style("band");

        for (int row = fromRow; row <= toRow; row++) {

            Cell cell = getOrCreateCell(row, col);
            cell.setCellStyle(bandStyle);
        }
    }

    // 为数据区域设置淡灰外框（仅四周，无内边框）。

    public void setDataBorder(int fromRow, int toRow, int fromCol, int toCol) {

        for (int row = fromRow; row <= toRow; row++) {

            for (int col = fromCol; col <= toCol; col++) {

                Cell cell = getOrCreateCell(row, col);
                CellStyle base = cell.getCellStyle();
                CellStyle bordered = style("bordered");
                bordered.cloneStyleFrom(base);
                setBorder(bordered, row == fromRow, row == toRow, col == fromCol, col == toCol);
                cell.setCellStyle(bordered);
            }
        }
    }

    // 合并指定区域单元格。

    public void mergeCells(int firstRow, int lastRow, int firstCol, int lastCol, Object content) {

        writer.merge(firstRow, lastRow, firstCol, lastCol, content, false);
        CellStyle style = style("moduleTitle");

        for (int row = firstRow; row <= lastRow; row++) {

            for (int col = firstCol; col <= lastCol; col++) {

                Cell cell = getOrCreateCell(row, col);
                cell.setCellStyle(style);
            }
        }
    }

    public void setColumnWidth(int col, int width) {

        sheet.setColumnWidth(col, width * 256);
    }

    // ==================== 内部方法 ====================

    private Cell getOrCreateCell(int row, int col) {

        org.apache.poi.ss.usermodel.Row r = sheet.getRow(row);

        // 判空处理
        if (r == null) {

            r = sheet.createRow(row);
        }

        Cell cell = r.getCell(col);

        // 判空处理
        if (cell == null) {

            cell = r.createCell(col);
        }

        return cell;
    }

    private CellStyle style(String key) {

        return styleCache.computeIfAbsent(key, this::buildStyle);
    }

    private CellStyle buildStyle(String key) {

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        // 按角色或类型分发
        switch (key) {

            case "moduleTitle" -> {

                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                setFillColor(style, MODULE_TITLE_BG);
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setFont(font(true, WHITE));
            }

            case "header" -> {

                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                setFillColor(style, HEADER_BG);
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setFont(font(true, BLACK));
            }

            case "data" -> {

                style.setAlignment(HorizontalAlignment.LEFT);
                style.setWrapText(true);
                style.setFont(font(false, BLACK));
            }

            case "band" -> {

                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                setFillColor(style, BAND_COLOR);
            }

            case "bordered" -> {

                style.setWrapText(true);
                style.setFont(font(false, BLACK));
            }

            default -> style.setWrapText(true);
        }

        return style;
    }

    private void setFillColor(CellStyle style, Color color) {

        ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style)
                .setFillForegroundColor(new XSSFColor(color, null));
    }

    private Font font(boolean bold, Color color) {

        Font f = workbook.createFont();
        f.setBold(bold);
        f.setFontHeightInPoints((short) 11);
        ((org.apache.poi.xssf.usermodel.XSSFFont) f).setColor(new XSSFColor(color, null));
        return f;
    }

    private void setBorder(CellStyle style, boolean top, boolean bottom, boolean left, boolean right) {

        short gray = borderGrayIndex();

        // 条件分支
        if (top) {

            style.setBorderTop(BorderStyle.THIN);
            style.setTopBorderColor(gray);
        }

        // 条件分支
        if (bottom) {

            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(gray);
        }

        // 条件分支
        if (left) {

            style.setBorderLeft(BorderStyle.THIN);
            style.setLeftBorderColor(gray);
        }

        // 条件分支
        if (right) {

            style.setBorderRight(BorderStyle.THIN);
            style.setRightBorderColor(gray);
        }
    }

    private short borderGrayIndex() {

        return IndexedColors.GREY_25_PERCENT.getIndex();
    }

    private String safeString(Object o) {

        return o == null ? "" : String.valueOf(o);
    }

    private void setCellValue(Cell cell, Object value) {

        // 判空处理
        if (value == null) {

            cell.setCellValue("");
            return;
        }

        // 条件分支
        if (value instanceof Number num) {

            cell.setCellValue(num.doubleValue());
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }
}
