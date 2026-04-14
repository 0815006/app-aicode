package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.*;

public class GaussTableDataExport {

    public static void main(String[] args) {
        final Logger logger = LogManager.getLogger(GaussTableDataExport.class);
        logger.info("This is an info message");
        logger.error("This is an error message");


        // 数据库连接信息
        String url = "jdbc:postgresql://82.204.253.31:8000/collection";
        String user = "collection_app";
        String password = "ColApp2025!";

        // 表名
        String tableName = "col_current_month_data";

        // 加载 PostgreSQL JDBC 驱动
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC Driver not found.");
            e.printStackTrace();
            return;
        }

        //查询表数据导出到excel
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM collection."+tableName+" limit 100")) {

            // 将 ResultSet 转换为 Excel 文件
            GaussCommon gaussCommon = new GaussCommon();
            gaussCommon.convertResultSetToExcel(rs, "Export表数据前100_"+tableName+".xlsx");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

}
