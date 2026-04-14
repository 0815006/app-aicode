package org.example;

import java.sql.*;
import java.io.*;
import java.util.*;

public class TDSQLTableExporter {

    private static final String JDBC_URL = "jdbc:mysql://82.214.22.212:15230/csas_scan";
    private static final String USERNAME = "aicoding";
    private static final String PASSWORD = "NmSql#2024!!!";

    public static void exportTableStructure(String outputPath) {
        Connection conn = null;
        try {
            // 建立连接
            conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);

            // 获取所有表名
            List<String> tableNames = getAllTableNames(conn);
            System.out.println(tableNames);
            // 导出每个表的结构
            for (String tableName : tableNames) {
                exportSingleTableStructure(conn, tableName, outputPath);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 获取所有表名
    private static List<String> getAllTableNames(Connection conn) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();

        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableSchema = rs.getString("TABLE_SCHEM");
                if (tableSchema != null && !tableSchema.equals("information_schema")) {
                    tableNames.add(tableName);
                }
            }
        }
        return tableNames;
    }

    // 导出单个表结构
    private static void exportSingleTableStructure(Connection conn, String tableName, String outputPath)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SHOW CREATE TABLE ").append(tableName).append(";");

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString());
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                String createTableSQL = rs.getString(2);
                writeToFile(createTableSQL, tableName, outputPath);
            }
        }
    }

    // 写入文件
    private static void writeToFile(String content, String tableName, String outputPath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath + "/" + tableName + ".sql"))) {
            System.out.println(outputPath + "/" + tableName + ".sql");
            writer.println("-- Table structure for " + tableName);
            writer.println(content);
            writer.println(";");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
