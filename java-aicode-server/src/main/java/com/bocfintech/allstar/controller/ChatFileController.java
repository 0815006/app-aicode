package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.ChatFile;
import com.bocfintech.allstar.service.ChatFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/chat/file")
@Api(tags = "聊天文件接口")
@Slf4j
public class ChatFileController {

    @Autowired
    private ChatFileService chatFileService;

    @Value("${chat.upload-path}")
    private String uploadPath;

    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public ResultBean<ChatFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") String uploaderId,
            @RequestParam("uploaderName") String uploaderName) {
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

            // 存储文件名：原文件名 + 时间戳
            String storageName = baseName + "_" + System.currentTimeMillis() + extension;
            File dest = new File(uploadPath + storageName);
            file.transferTo(dest);

            ChatFile chatFile = new ChatFile();
            chatFile.setFileName(originalFilename);
            chatFile.setStorageName(storageName);
            chatFile.setFileSize(file.getSize());
            chatFile.setUploaderId(uploaderId);
            chatFile.setUploaderName(uploaderName);
            chatFile.setCreateTime(new Date());

            chatFileService.save(chatFile);

            return ResultBean.success(chatFile);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResultBean.error("文件上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取文件列表")
    public ResultBean<List<ChatFile>> list() {
        List<ChatFile> list = chatFileService.list(new LambdaQueryWrapper<ChatFile>()
                .orderByDesc(ChatFile::getCreateTime));
        return ResultBean.success(list);
    }

    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除文件")
    public ResultBean<String> delete(@PathVariable Long id, @RequestParam String uploaderId) {
        ChatFile chatFile = chatFileService.getById(id);
        if (chatFile == null) {
            return ResultBean.error("文件不存在");
        }
        if (!chatFile.getUploaderId().equals(uploaderId)) {
            return ResultBean.error("只有上传者可以删除文件");
        }

        // 删除物理文件
        File file = new File(uploadPath + chatFile.getStorageName());
        if (file.exists()) {
            file.delete();
        }

        chatFileService.removeById(id);
        return ResultBean.success("删除成功");
    }

    @GetMapping("/download/{id}")
    @ApiOperation("下载文件")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        ChatFile chatFile = chatFileService.getById(id);
        if (chatFile == null) {
            return;
        }

        File file = new File(uploadPath + chatFile.getStorageName());
        if (!file.exists()) {
            return;
        }

        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(chatFile.getFileName(), "UTF-8"));
            
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
}
