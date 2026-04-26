package com.bocfintech.allstar;

import com.bocfintech.allstar.service.ChatMessageService;
import com.bocfintech.allstar.websocket.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableScheduling // 启用定时任务
@EnableAsync // 启用异步任务支持
@ComponentScan(basePackages = "com.bocfintech.allstar") // 确保包扫描正确
public class Application {
    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        // 注入 ChatMessageService 到 WebSocketServer
        ChatMessageService chatMessageService = context.getBean(ChatMessageService.class);
        WebSocketServer.setChatMessageService(chatMessageService);
    }
}
