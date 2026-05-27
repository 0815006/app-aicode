package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/text-extract")
@Api(tags = "文本提取文件接口")
@Slf4j
public class ExtractFileController {

    @Value("${text-extract.upload-path}")
    private String uploadPath;

    /** 允许的文件扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(".docx", ".xlsx", ".xls"));

    @PostMapping("/upload")
    @ApiOperation("上传Word或Excel文件")
    public ResultBean<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResultBean.error("文件名不能为空");
        }

        // 校验扩展名
        String lowerName = originalFilename.toLowerCase();
        boolean allowed = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return ResultBean.error("仅支持上传 Word (.docx) 和 Excel (.xlsx/.xls) 文件");
        }

        try {
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 保留原文件名，若同名则追加时间戳
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String storedName = originalFilename;
            File dest = new File(uploadPath + storedName);
            if (dest.exists()) {
                storedName = baseName + "_" + System.currentTimeMillis() + extension;
                dest = new File(uploadPath + storedName);
            }

            file.transferTo(dest);
            log.info("文件上传成功: {}", storedName);

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", storedName);
            result.put("originalName", originalFilename);
            result.put("fileSize", dest.length());
            result.put("url", "/api/uploads/text-extract/" + storedName);
            return ResultBean.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResultBean.error("文件上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/files")
    @ApiOperation("获取已上传的文件列表")
    public ResultBean<List<Map<String, Object>>> listFiles() {
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            return ResultBean.success(Collections.emptyList());
        }

        File[] files = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".docx") || lower.endsWith(".xlsx") || lower.endsWith(".xls");
        });

        if (files == null) {
            return ResultBean.success(Collections.emptyList());
        }

        List<Map<String, Object>> fileList = new ArrayList<>();
        for (File file : files) {
            Map<String, Object> info = new HashMap<>();
            info.put("fileName", file.getName());
            info.put("fileSize", file.length());
            info.put("lastModified", file.lastModified());
            String ext = "";
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                ext = "excel";
            } else if (lower.endsWith(".docx")) {
                ext = "word";
            }
            info.put("fileType", ext);
            info.put("url", "/api/uploads/text-extract/" + file.getName());
            fileList.add(info);
        }

        // 按修改时间倒序
        fileList.sort((a, b) -> Long.compare((Long) b.get("lastModified"), (Long) a.get("lastModified")));
        return ResultBean.success(fileList);
    }

    @DeleteMapping("/file")
    @ApiOperation("删除指定文件")
    public ResultBean<String> deleteFile(@RequestParam("fileName") String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ResultBean.error("文件名不能为空");
        }

        // 防止路径穿越
        String safeName = new File(fileName).getName();
        File file = new File(uploadPath + safeName);
        if (!file.exists()) {
            return ResultBean.error("文件不存在");
        }

        if (file.delete()) {
            log.info("文件删除成功: {}", safeName);
            return ResultBean.success("删除成功");
        } else {
            return ResultBean.error("删除失败");
        }
    }
}
