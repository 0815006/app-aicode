package com.bocfintech.allstar.bean;

public class ImportResult {
    private boolean success;
    private String message;
    private String sqlFilePath;

    public ImportResult(boolean success, String message, String sqlFilePath) {
        this.success = success;
        this.message = message;
        this.sqlFilePath = sqlFilePath;
    }

    // getter和setter方法
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSqlFilePath() {
        return sqlFilePath;
    }

    public void setSqlFilePath(String sqlFilePath) {
        this.sqlFilePath = sqlFilePath;
    }
}
