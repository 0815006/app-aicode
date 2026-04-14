package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class GaussCommon {

    //表查询数据导出到excel
    public static void convertResultSetToExcel(ResultSet rs, String excelFilePath) throws SQLException, IOException {
        // 创建一个新的工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 获取 ResultSet 的元数据
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 创建表头行
        Row headerRow = sheet.createRow(0);
        for (int i = 1; i <= columnCount; i++) {
            Cell cell = headerRow.createCell(i - 1);
            cell.setCellValue(metaData.getColumnName(i));
        }

        // 创建日期格式
        CellStyle dateCellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd hh:mm:ss"));


        // 创建数据行
        int rowCount = 1;
        while (rs.next()) {
            Row row = sheet.createRow(rowCount++);
            for (int i = 1; i <= columnCount; i++) {
                Cell cell = row.createCell(i - 1);
                if (rs.getObject(i) == null) {
                    cell.setCellValue("");
                } else {
                    switch (metaData.getColumnType(i)) {
                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.NVARCHAR:
                        case Types.NCHAR:
                            cell.setCellValue(rs.getString(i));
                            break;
                        case Types.INTEGER:
                            cell.setCellValue(rs.getInt(i));
                            break;
                        case Types.DOUBLE:
                            cell.setCellValue(rs.getDouble(i));
                            break;
                        case Types.FLOAT:
                            cell.setCellValue(rs.getFloat(i));
                            break;
                        case Types.BOOLEAN:
                            cell.setCellValue(rs.getBoolean(i));
                            break;
                        case Types.DATE:
                            cell.setCellValue(rs.getDate(i));
                            cell.setCellStyle(dateCellStyle);
                            break;
                        case Types.TIMESTAMP:
                            cell.setCellValue(rs.getTimestamp(i));
                            cell.setCellStyle(dateCellStyle);
                            break;
                        default:
//                        cell.setCellValue(rs.getObject(i).toString());
                            //nvarchar2类型例外
                            cell.setCellValue("-");
                            break;
                    }
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }

        // 将工作簿写入文件
        try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
            workbook.write(fileOut);
        }

        // 关闭工作簿
        workbook.close();
    }

}
