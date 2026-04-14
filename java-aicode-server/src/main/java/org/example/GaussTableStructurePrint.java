package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.*;

public class GaussTableStructurePrint {

    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger(GaussTableStructurePrint.class);
        logger.info("This is an info message");
//        logger.error("This is an error message");


        // 数据库连接信息 postgresql
        String url = "jdbc:mysql://82.214.22.212:15230/csas_scan";
        String user = "aicoding";
        String password = "NmSql#2024!!!";

        // 表名
        String tableName = "scan_dbstore_config";

        // 加载 PostgreSQL JDBC 驱动
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver not found.");
            e.printStackTrace();
            return;
        }


        // 连接数据库
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            // 获取数据库元数据
            DatabaseMetaData metaData = connection.getMetaData();

            // 获取表的列信息
            try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                // 将 ResultSet 转换为 Excel 文件
                GaussCommon gaussCommon = new GaussCommon();
                gaussCommon.convertResultSetToExcel(columns, "Print表结构"+tableName+".xlsx");


//                while (columns.next()) {
//                    String columnName = columns.getString("COLUMN_NAME");
//                    String columnType = columns.getString("TYPE_NAME");
//                    int columnSize = columns.getInt("COLUMN_SIZE");
//                    int decimalDigits = columns.getInt("DECIMAL_DIGITS");
//                    int nullable = columns.getInt("NULLABLE");
//                    String remarks = columns.getString("REMARKS");
//
//                    System.out.println("Column Name: " + columnName);
//                    System.out.println("Column Type: " + columnType);
//                    System.out.println("Column Size: " + columnSize);
//                    System.out.println("Decimal Digits: " + decimalDigits);
//                    System.out.println("Nullable: " + (nullable == DatabaseMetaData.columnNullable ? "YES" : "NO"));
//                    System.out.println("Remarks: " + remarks);
//                    System.out.println("-----------------------------");
//                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 获取表的主键信息
            try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName)) {
                System.out.println("Primary Keys for table: " + tableName);
                while (primaryKeys.next()) {
                    String columnName = primaryKeys.getString("COLUMN_NAME");
                    System.out.println("Primary Key Column: " + columnName);
                }
            }

            // 获取表的外键信息
            try (ResultSet foreignKeys = metaData.getImportedKeys(null, null, tableName)) {
                System.out.println("Foreign Keys for table: " + tableName);
                while (foreignKeys.next()) {
                    String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                    String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                    String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
                    System.out.println("Foreign Key Column: " + fkColumnName);
                    System.out.println("References Table: " + pkTableName);
                    System.out.println("References Column: " + pkColumnName);
                    System.out.println("-----------------------------");
                }

            }

        } catch (SQLException e) {
            System.out.println("SQL Exception occurred.");
            e.printStackTrace();
        }
    }

}
