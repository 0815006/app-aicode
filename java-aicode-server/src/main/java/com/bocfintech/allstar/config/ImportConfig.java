package com.bocfintech.allstar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "import")
public class ImportConfig {
    private List<String> tables;
    private String inputPath;
    private String tempPath;

    // getter和setter方法
    public List<String> getTables() { return tables; }
    public void setTables(List<String> tables) { this.tables = tables; }

    public String getInputPath() { return inputPath; }
    public void setInputPath(String inputPath) { this.inputPath = inputPath; }

    public String getTempPath() { return tempPath; }
    public void setTempPath(String tempPath) { this.tempPath = tempPath; }
}
