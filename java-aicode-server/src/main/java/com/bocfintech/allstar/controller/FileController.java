package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@Api(tags = "文件上传接口")
@Slf4j
public class FileController {

    @Value("${vote.upload-path}")
    private String uploadPath;

    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public ResultBean<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        try {
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            String baseName = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                int lastDotIndex = originalFilename.lastIndexOf(".");
                extension = originalFilename.substring(lastDotIndex);
                baseName = originalFilename.substring(0, lastDotIndex);
            } else {
                baseName = originalFilename;
            }
            
            // 使用原文件名 + 时间戳
            String fileName = baseName + "_" + System.currentTimeMillis() + extension;
            File dest = new File(uploadPath + fileName);
            file.transferTo(dest);

            // 返回可访问的 URL 路径，统一带上 /api 前缀
            String fileUrl = "/api/uploads/vote/" + fileName;
            return ResultBean.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResultBean.error("文件上传失败：" + e.getMessage());
        }
    }
}
