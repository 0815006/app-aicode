package com.bocfintech.allstar.service;

import com.bocfintech.allstar.bean.ExportResult;
import com.bocfintech.allstar.config.ExportConfig;
import com.bocfintech.allstar.config.MysqlConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseExportService {
    
    @Autowired
    private MysqlConfig mysqlConfig;
    
    @Autowired
    private ExportConfig exportConfig;
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExportService.class);
    
    /**
     * 导出数据库表到SQL文件
     * @param outputPath 输出路径
     * @return 导出结果
     */
    public ExportResult exportDatabase(String outputPath) {
        try {
            // 验证输出路径
            if (outputPath == null || outputPath.trim().isEmpty()) {
                return new ExportResult(false, "输出路径不能为空", null);
            }
            
            // 确保输出目录存在
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                logger.info("创建输出目录: {}", outputPath);
            }
            
            // 生成文件名
            String fileName = "selected_tables-" + LocalDate.now().toString() + ".sql";
            String fullFilePath = outputPath + File.separator + fileName;
            
            // 构建mysqldump命令
            StringBuilder command = new StringBuilder();
            command.append("mysqldump ")
                   .append("-h ").append(mysqlConfig.getHost()).append(" ")
                   .append("-u").append(mysqlConfig.getUserName()).append(" ")
                   .append("-p").append(mysqlConfig.getPassword()).append(" ")
                   .append("--single-transaction ")
                   .append("--set-gtid-purged=OFF ")
                   .append("--default-character-set=").append(mysqlConfig.getCharset()).append(" ")
                   .append(mysqlConfig.getDatabase()).append(" ");
            // 添加表名
            for (int i = 0; i < exportConfig.getTables().size(); i++) {
                if (i > 0) {
                    command.append(" ");
                }
                command.append(exportConfig.getTables().get(i));
            }
            command.append(" ").append("2>nul");
            System.out.println("export command:"+command);

            logger.info("执行导出命令: {}", command.toString());
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出并写入文件
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(fullFilePath))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("数据库导出成功，文件路径: {}", fullFilePath);
                return new ExportResult(true, "导出成功", fullFilePath);
            } else {
                logger.error("数据库导出失败，退出码: {}", exitCode);
                return new ExportResult(false, "导出失败，退出码: " + exitCode, null);
            }
            
        } catch (IOException e) {
            logger.error("IO异常导致导出失败", e);
            return new ExportResult(false, "IO异常: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            logger.error("导出过程被中断", e);
            Thread.currentThread().interrupt();
            return new ExportResult(false, "导出被中断: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("导出过程中发生未知异常", e);
            return new ExportResult(false, "导出异常: " + e.getMessage(), null);
        }
    }
    
    /**
     * 导出数据库表到SQL文件（带自定义表列表）
     * @param outputPath 输出路径
     * @param customTables 自定义表列表
     * @return 导出结果
     */
    public ExportResult exportDatabaseWithCustomTables(String outputPath, List<String> customTables) {
        try {
            // 验证参数
            if (outputPath == null || outputPath.trim().isEmpty()) {
                return new ExportResult(false, "输出路径不能为空", null);
            }
            
            if (customTables == null || customTables.isEmpty()) {
                return new ExportResult(false, "表列表不能为空", null);
            }
            
            // 确保输出目录存在
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                logger.info("创建输出目录: {}", outputPath);
            }
            
            // 生成文件名
            String fileName = "selected_tables-" + LocalDate.now().toString() + ".sql";
            String fullFilePath = outputPath + File.separator + fileName;
            
            // 构建mysqldump命令
            StringBuilder command = new StringBuilder();
            command.append("mysqldump ")
                   .append("-h ").append(mysqlConfig.getHost()).append(" ")
                   .append("-u").append(mysqlConfig.getUserName()).append(" ")
                   .append("-p").append(mysqlConfig.getPassword()).append(" ")
                   .append("--single-transaction ")
                   .append("--set-gtid-purged=OFF ")
                   .append("--default-character-set=").append(mysqlConfig.getCharset()).append(" ")
                   .append(mysqlConfig.getDatabase()).append(" ");
            
            // 添加自定义表名
            for (int i = 0; i < customTables.size(); i++) {
                if (i > 0) {
                    command.append(" ");
                }
                command.append(customTables.get(i));
            }
            command.append(" ").append("2>nul");
            System.out.println("export command:"+command);
            logger.info("执行自定义导出命令: {}", command.toString());
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出并写入文件
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(fullFilePath))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("数据库自定义导出成功，文件路径: {}", fullFilePath);
                return new ExportResult(true, "导出成功", fullFilePath);
            } else {
                logger.error("数据库自定义导出失败，退出码: {}", exitCode);
                return new ExportResult(false, "导出失败，退出码: " + exitCode, null);
            }
            
        } catch (IOException e) {
            logger.error("IO异常导致导出失败", e);
            return new ExportResult(false, "IO异常: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            logger.error("导出过程被中断", e);
            Thread.currentThread().interrupt();
            return new ExportResult(false, "导出被中断: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("导出过程中发生未知异常", e);
            return new ExportResult(false, "导出异常: " + e.getMessage(), null);
        }
    }
    
    /**
     * 获取所有可用表
     * @return 表名列表
     */
    public List<String> getAllAvailableTables() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + mysqlConfig.getHost() + ":" + mysqlConfig.getPort() + "/" + mysqlConfig.getDatabase();
            Connection connection = DriverManager.getConnection(url, mysqlConfig.getUserName(), mysqlConfig.getPassword());
            
            List<String> tables = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(mysqlConfig.getDatabase(), null, "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
            
            rs.close();
            connection.close();
            return tables;
            
        } catch (Exception e) {
            logger.error("获取表列表失败", e);
            return new ArrayList<>();
        }
    }
}
