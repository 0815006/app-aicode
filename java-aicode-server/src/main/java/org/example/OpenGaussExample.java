package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OpenGaussExample {
    public static void main(String[] args) {
        // 显式加载 OpenGauss JDBC 驱动
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("postgresql JDBC 驱动未找到");
            e.printStackTrace();
            return;
        }

        // 连接字符串
        String url = "jdbc:postgresql://82.204.253.31:8000/asset";
        String user = "asset_app";
        String password = "AstApp2025!";

        // 获取连接
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("成功连接到 gauss 数据库");
            // 在这里执行你的 SQL 操作
        } catch (SQLException e) {
            System.err.println("连接到 gauss 数据库时出错");
            e.printStackTrace();
        }
    }
}
