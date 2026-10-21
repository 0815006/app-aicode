package com.bocfintech.allstar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${text-extract.upload-path}")
    private String textExtractUploadPath;

    @Value("${chat.upload-path}")
    private String chatUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /api/uploads/text-extract/** 映射到文本提取文件目录
        registry.addResourceHandler("/api/uploads/text-extract/**")
                .addResourceLocations("file:" + textExtractUploadPath);
         
        // 将 /api/uploads/chat/** 映射到本地物理路径
        registry.addResourceHandler("/api/uploads/chat/**")
                .addResourceLocations("file:" + chatUploadPath);
    }
}
