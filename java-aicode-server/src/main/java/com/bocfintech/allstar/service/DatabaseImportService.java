package com.bocfintech.allstar.service;

import com.bocfintech.allstar.bean.ImportResult;
import com.bocfintech.allstar.config.ImportConfig;
import com.bocfintech.allstar.config.MysqlConfig;
import com.bocfintech.allstar.config.TdsqlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DatabaseImportService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseImportService.class);

    @Autowired
    private ImportConfig importConfig;

    @Autowired
    private TdsqlConfig tdsqlConfig;

    /**
     * 导入指定SQL文件
     * @param sqlFilePath SQL文件路径
     * @return 导入结果
     */
    public ImportResult importDatabase(String sqlFilePath) {
        try {
            // 验证SQL文件是否存在
            File sqlFile = new File(sqlFilePath);
            if (!sqlFile.exists()) {
                return new ImportResult(false, "SQL文件不存在: " + sqlFilePath, null);
            }

            // 获取TDSQL配置信息
            if (tdsqlConfig == null ||
                    tdsqlConfig.getHost() == null ||
                    tdsqlConfig.getUserName() == null ||
                    tdsqlConfig.getPassword() == null ||
                    tdsqlConfig.getDatabase() == null) {
                return new ImportResult(false, "数据库配置信息不完整", null);
            }

            // 构建mysql导入命令
            String command = String.format(
                    "mysql -h%s -P%s -u%s -p%s --default-character-set=%s %s < %s",
                    tdsqlConfig.getHost(),
                    tdsqlConfig.getPort(),
                    tdsqlConfig.getUserName(),
                    tdsqlConfig.getPassword(),
                    tdsqlConfig.getCharset(),
                    tdsqlConfig.getDatabase(),
                    sqlFilePath
            );
            System.out.println("import command:"+command);

            // 执行导入命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command);
            Process process = processBuilder.start();

            // 等待命令执行完成
            boolean finished = process.waitFor(3000, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return new ImportResult(false, "导入命令执行超时", null);
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return new ImportResult(true, "导入成功", sqlFilePath);
            } else {
                // 读取错误输出
                String errorOutput = readProcessError(process);
                return new ImportResult(false, "导入失败，错误代码: " + exitCode + ", 错误信息: " + errorOutput, sqlFilePath);
            }

        } catch (IOException e) {
            return new ImportResult(false, "执行导入命令时发生IO异常: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ImportResult(false, "导入命令执行被中断: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ImportResult(false, "服务器内部错误: " + e.getMessage(), null);
        }
    }

    /**
     * 导入指定目录下所有SQL文件
     * @return 导入结果
     */
    public ImportResult importAllSqlFiles() {
        try {
            String inputPath = importConfig.getInputPath();
            if (inputPath == null || inputPath.isEmpty()) {
                return new ImportResult(false, "输入路径配置为空", null);
            }

            File directory = new File(inputPath);
            if (!directory.exists() || !directory.isDirectory()) {
                return new ImportResult(false, "输入路径不存在或不是目录: " + inputPath, null);
            }

            // 获取所有SQL文件
            File[] sqlFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".sql"));
            if (sqlFiles == null || sqlFiles.length == 0) {
                return new ImportResult(false, "在目录中未找到SQL文件: " + inputPath, null);
            }

            // 逐个导入SQL文件
            for (File sqlFile : sqlFiles) {
                ImportResult result = importDatabase(sqlFile.getAbsolutePath());
                if (!result.isSuccess()) {
                    return result;
                }
            }

            return new ImportResult(true, "所有SQL文件导入成功", inputPath);

        } catch (Exception e) {
            return new ImportResult(false, "导入所有SQL文件时发生错误: " + e.getMessage(), null);
        }
    }

    /**
     * 导入指定表的SQL文件
     * @param tableNames 表名列表
     * @return 导入结果
     */
    public ImportResult importTables(List<String> tableNames) {
        try {
            String inputPath = importConfig.getInputPath();
            if (inputPath == null || inputPath.isEmpty()) {
                return new ImportResult(false, "输入路径配置为空", null);
            }

            File directory = new File(inputPath);
            if (!directory.exists() || !directory.isDirectory()) {
                return new ImportResult(false, "输入路径不存在或不是目录: " + inputPath, null);
            }

            // 查找匹配的SQL文件
            List<File> sqlFiles = Arrays.stream(directory.listFiles())
                    .filter(file -> file.getName().toLowerCase().endsWith(".sql"))
                    .filter(file -> {
                        String fileName = file.getName();
                        // 检查文件名是否包含指定表名
                        return tableNames.stream().anyMatch(tableName -> fileName.contains(tableName));
                    })
                    .collect(Collectors.toList());

            if (sqlFiles.isEmpty()) {
                return new ImportResult(false, "未找到匹配的SQL文件", null);
            }

            // 逐个导入匹配的SQL文件
            for (File sqlFile : sqlFiles) {
                ImportResult result = importDatabase(sqlFile.getAbsolutePath());
                if (!result.isSuccess()) {
                    return result;
                }
            }

            return new ImportResult(true, "指定表的SQL文件导入成功", sqlFiles.toString());

        } catch (Exception e) {
            return new ImportResult(false, "导入指定表SQL文件时发生错误: " + e.getMessage(), null);
        }
    }

    /**
     * 获取所有可用表
     * @return 表名列表
     */
    public List<String> getAllAvailableTables() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + tdsqlConfig.getHost() + ":" + tdsqlConfig.getPort() + "/" + tdsqlConfig.getDatabase();
            Connection connection = DriverManager.getConnection(url, tdsqlConfig.getUserName(), tdsqlConfig.getPassword());

            List<String> tables = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(tdsqlConfig.getDatabase(), null, "%", new String[]{"TABLE"});

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

    /**
     * 读取进程的错误输出
     */
    private String readProcessError(Process process) {
        try {
            StringBuilder errorOutput = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A");
            if (scanner.hasNext()) {
                errorOutput.append(scanner.next());
            }
            scanner.close();
            return errorOutput.toString();
        } catch (Exception e) {
            return "无法读取错误信息: " + e.getMessage();
        }
    }
}
