package com.bocfintech.allstar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${vote.upload-path}")
    private String uploadPath;

    @Value("${chat.upload-path}")
    private String chatUploadPath;

    @Value("${app.crawl.image-base-path}")
    private String crawlImageBasePath;

    @Value("${app.crawl.video-base-path}")
    private String crawlVideoBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /api/uploads/vote/** 映射到本地物理路径
        registry.addResourceHandler("/api/uploads/vote/**")
                .addResourceLocations("file:" + uploadPath);
        
        // 将 /api/uploads/chat/** 映射到本地物理路径
        registry.addResourceHandler("/api/uploads/chat/**")
                .addResourceLocations("file:" + chatUploadPath);

        // 将 /api/images/img/** 映射到图片抓取目录
        registry.addResourceHandler("/api/images/img/**")
                .addResourceLocations("file:" + crawlImageBasePath);

        // 将 /api/images/vid/** 映射到视频抓取目录
        registry.addResourceHandler("/api/images/vid/**")
                .addResourceLocations("file:" + crawlVideoBasePath);
    }
}
