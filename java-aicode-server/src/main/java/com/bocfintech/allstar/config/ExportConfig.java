package com.bocfintech.allstar.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "export")
public class ExportConfig {
    private List<String> tables;
    private String outputPath;
    private String tempPath;

    // getter和setter方法
    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public String getTempPath() { return tempPath; }
    public void setTempPath(String tempPath) { this.tempPath = tempPath; }
}
