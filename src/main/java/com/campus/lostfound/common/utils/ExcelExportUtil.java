package com.campus.lostfound.common.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * Excel 导出工具类
 * 基于 Apache POI 的 SXSSF（流式写入），适用于大数据量导出，内存占用低
 */
public final class ExcelExportUtil {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportUtil.class);

    /**
     * 默认内存中保留的行数（超出会写入磁盘临时文件）
     */
    private static final int WINDOW_SIZE = 100;

    private ExcelExportUtil() {
    }

    /**
     * 导出 Excel 到 HTTP 响应输出流
     *
     * @param response  HTTP 响应
     * @param fileName  文件名（不含扩展名，中文会自动 URL 编码）
     * @param headers   表头
     * @param dataRows  数据行（每行为一个 Object 列表，按表头顺序对应）
     * @throws IOException 写入异常
     */
    public static void exportToExcel(HttpServletResponse response, String fileName,
                                     List<String> headers, List<List<Object>> dataRows) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW_SIZE);
        try {
            Sheet sheet = workbook.createSheet("Sheet1");

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 写表头
            Row headerRow = sheet.createRow(0);
            if (headers != null) {
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(i, 18 * 256);
                }
            }

            // 写数据行
            if (dataRows != null) {
                for (int r = 0; r < dataRows.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    List<Object> rowData = dataRows.get(r);
                    if (rowData == null) {
                        continue;
                    }
                    for (int c = 0; c < rowData.size(); c++) {
                        setCellValue(row.createCell(c), rowData.get(c));
                    }
                }
            }

            // 设置响应头（处理中文文件名）
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=UTF-8''" + encodedFileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            // 清理磁盘临时文件并关闭工作簿
            workbook.dispose();
            workbook.close();
        }
        log.info("Excel 导出完成：{}，共 {} 行数据", fileName, dataRows == null ? 0 : dataRows.size());
    }

    /**
     * 根据值类型设置单元格内容
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
