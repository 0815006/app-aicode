package com.bocfintech.allstar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tdsql")
public class TdsqlConfig {
    private String host;
    private int port;
    private String userName;
    private String password;
    private String database;
    private String charset;

    // getter和setter方法
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
}
