package com.bocfintech.allstar.bean;

public class ExportResult {
    private boolean success;
    private String message;
    private String filePath;
    
    public ExportResult() {}
    
    public ExportResult(boolean success, String message, String filePath) {
        this.success = success;
        this.message = message;
        this.filePath = filePath;
    }
    
    // getter和setter方法
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
