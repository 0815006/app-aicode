package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.SharedFile;
import com.bocfintech.allstar.service.SharedFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/file-share")
@Api(tags = "文件共享接口")
@Slf4j
public class SharedFileController {

    @Autowired
    private SharedFileService sharedFileService;

    @Value("${file-share.root-path}")
    private String rootPath;

    // ==================== 子目录管理 ====================

    @GetMapping("/directories")
    @ApiOperation("获取所有子目录列表")
    public ResultBean<List<Map<String, Object>>> listDirectories() {
        List<Map<String, Object>> result = new ArrayList<>();
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File dir : subDirs) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", dir.getName());
                // 统计该子目录文件数
                File[] files = dir.listFiles(File::isFile);
                item.put("fileCount", files != null ? files.length : 0);
                result.add(item);
            }
        }
        return ResultBean.success(result);
    }

    @PostMapping("/directory")
    @ApiOperation("创建子目录")
    public ResultBean<String> createDirectory(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResultBean.error("子目录名称不能为空");
        }
        // 过滤非法字符
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        File dir = new File(rootPath + safeName);
        if (dir.exists()) {
            return ResultBean.error("子目录已存在");
        }
        if (dir.mkdirs()) {
            return ResultBean.success("创建成功");
        }
        return ResultBean.error("创建失败");
    }

    @DeleteMapping("/directory")
    @ApiOperation("删除子目录及其所有文件")
    public ResultBean<String> deleteDirectory(@RequestParam String name) {
        File dir = new File(rootPath + name);
        if (!dir.exists() || !dir.isDirectory()) {
            return ResultBean.error("子目录不存在");
        }
        // 删除物理文件
        deleteDirRecursively(dir);
        // 删除数据库记录
        sharedFileService.remove(new LambdaQueryWrapper<SharedFile>()
                .eq(SharedFile::getSubDirectoryName, name));
        return ResultBean.success("删除成功");
    }

    private void deleteDirRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirRecursively(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    // ==================== 文件管理 ====================

    @GetMapping("/files")
    @ApiOperation("按子目录查询文件列表")
    public ResultBean<List<SharedFile>> listFiles(@RequestParam String subDirectoryName) {
        List<SharedFile> list = sharedFileService.list(new LambdaQueryWrapper<SharedFile>()
                .eq(SharedFile::getSubDirectoryName, subDirectoryName)
                .orderByDesc(SharedFile::getCreatedAt));
        return ResultBean.success(list);
    }

    @PostMapping("/upload")
    @ApiOperation("上传文件到子目录")
    public ResultBean<SharedFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subDirectoryName") String subDirectoryName,
            @RequestParam("uploaderId") String uploaderId,
            HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }
        if (subDirectoryName == null || subDirectoryName.trim().isEmpty()) {
            return ResultBean.error("子目录名称不能为空");
        }

        try {
            // 确保子目录存在
            File subDir = new File(rootPath + subDirectoryName);
            if (!subDir.exists()) {
                subDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            // 物理存储路径：子目录下直接存原文件名（如重名则加时间戳）
            File dest = new File(subDir, originalFilename);
            if (dest.exists()) {
                String baseName = originalFilename;
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    int dot = originalFilename.lastIndexOf(".");
                    baseName = originalFilename.substring(0, dot);
                    extension = originalFilename.substring(dot);
                }
                dest = new File(subDir, baseName + "_" + System.currentTimeMillis() + extension);
            }

            file.transferTo(dest);

            String ip = getClientIp(request);

            SharedFile sharedFile = new SharedFile();
            sharedFile.setFileName(dest.getName());
            sharedFile.setFileSize(file.getSize());
            sharedFile.setSubDirectoryName(subDirectoryName);
            sharedFile.setPhysicalPath(dest.getAbsolutePath());
            sharedFile.setUploaderId(uploaderId);
            sharedFile.setUploaderIp(ip);
            sharedFile.setDownloadCount(0);
            sharedFile.setCreatedAt(new Date());
            sharedFile.setUpdatedAt(new Date());

            sharedFileService.save(sharedFile);

            return ResultBean.success(sharedFile);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResultBean.error("文件上传失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除文件")
    public ResultBean<String> delete(@PathVariable Long id, @RequestParam String uploaderId) {
        SharedFile sf = sharedFileService.getById(id);
        if (sf == null) {
            return ResultBean.error("文件不存在");
        }
        // 删除物理文件
        File physicalFile = new File(sf.getPhysicalPath());
        if (physicalFile.exists()) {
            physicalFile.delete();
        }
        sharedFileService.removeById(id);
        return ResultBean.success("删除成功");
    }

    @GetMapping("/download/{id}")
    @ApiOperation("下载文件")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        SharedFile sf = sharedFileService.getById(id);
        if (sf == null) {
            return;
        }

        File file = new File(sf.getPhysicalPath());
        if (!file.exists()) {
            return;
        }

        // 下载次数+1
        sf.setDownloadCount(sf.getDownloadCount() + 1);
        sharedFileService.updateById(sf);

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(sf.getFileName(), "UTF-8"));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            log.error("文件下载失败", e);
        }
    }

    // ==================== 工具方法 ====================

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
