package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/performance/file")
@Api(tags = "性能测试模板文件接口")
@Slf4j
public class PerformanceFileController {

    @Value("${performance.template-path}")
    private String templatePath;

    @PostMapping("/upload")
    @ApiOperation("上传模板文件")
    public ResultBean<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        try {
            File directory = new File(templatePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            File dest = new File(templatePath + originalFilename);
            file.transferTo(dest);

            return ResultBean.success("上传成功");
        } catch (IOException e) {
            log.error("模板文件上传失败", e);
            return ResultBean.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取模板文件列表")
    public ResultBean<List<TemplateFileInfo>> list() {
        List<TemplateFileInfo> fileList = new ArrayList<>();
        File directory = new File(templatePath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        TemplateFileInfo info = new TemplateFileInfo();
                        info.setFileName(file.getName());
                        info.setFileSize(file.length());
                        info.setLastModified(new Date(file.lastModified()));
                        fileList.add(info);
                    }
                }
            }
        }
        fileList.sort((a, b) -> b.getLastModified().compareTo(a.getLastModified()));
        return ResultBean.success(fileList);
    }

    @DeleteMapping("/delete")
    @ApiOperation("删除模板文件")
    public ResultBean<String> delete(@RequestParam String fileName) {
        File file = new File(templatePath + fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return ResultBean.success("删除成功");
            } else {
                return ResultBean.error("删除失败");
            }
        }
        return ResultBean.error("文件不存在");
    }

    @GetMapping("/download")
    @ApiOperation("下载模板文件")
    public void download(@RequestParam String fileName, HttpServletResponse response) {
        File file = new File(templatePath + fileName);
        if (!file.exists() || !file.isFile()) {
            return;
        }

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            log.error("模板文件下载失败", e);
        }
    }

    @GetMapping("/downloadByKeyword")
    @ApiOperation("按关键字下载模板文件")
    public void downloadByKeyword(@RequestParam String keyword, HttpServletResponse response) {
        File directory = new File(templatePath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().contains(keyword)) {
                        download(file.getName(), response);
                        return;
                    }
                }
            }
        }
    }

    @Data
    public static class TemplateFileInfo {
        private String fileName;
        private long fileSize;
        private Date lastModified;
    }
}
