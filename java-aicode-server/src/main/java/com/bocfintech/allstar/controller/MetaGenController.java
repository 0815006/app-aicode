package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.MetaEntityFile;
import com.bocfintech.allstar.entity.MetaFtpConfig;
import com.bocfintech.allstar.service.MetaEntityFileService;
import com.bocfintech.allstar.service.MetaFtpConfigService;
import com.bocfintech.allstar.service.MetaGenEngineService;
import com.bocfintech.allstar.service.MetaSequenceTrackerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/meta")
public class MetaGenController {

    @Autowired
    private MetaGenEngineService engineService;

    @Autowired
    private MetaEntityFileService entityFileService;

    @Autowired
    private MetaSequenceTrackerService seqService;

    @Autowired
    private MetaFtpConfigService ftpConfigService;

    // ===================== 执行与生成 =====================

    /**
     * 实时预览生成（同步，3行，不更新 tracker）
     */
    @PostMapping("/execute/preview/{modelId}")
    public ResultBean<String> preview(@PathVariable Long modelId,
                                       @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        try {
            String previewText = engineService.preview(modelId, empNo);
            return ResultBean.success(previewText);
        } catch (Exception e) {
            log.error("预览生成失败", e);
            return ResultBean.error("预览失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成任务（异步）
     */
    @PostMapping("/execute/generate")
    public ResultBean<MetaEntityFile> generate(@RequestBody GenerateRequest req,
                                                @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        try {
            MetaEntityFile record = engineService.generateAsync(req.getModelId(), req.getRowCount(), req.getBatchName(), empNo);
            return ResultBean.success(record);
        } catch (Exception e) {
            log.error("批量生成失败", e);
            return ResultBean.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 查询生成进度
     */
    @GetMapping("/execute/status/{taskId}")
    public ResultBean<MetaEntityFile> getStatus(@PathVariable Long taskId,
                                                 @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        MetaEntityFile record = engineService.getStatus(taskId);
        if (record == null) return ResultBean.error("任务不存在");
        return ResultBean.success(record);
    }

    /**
     * 获取生成历史
     */
    @GetMapping("/execute/history")
    public ResultBean<List<MetaEntityFile>> getHistory(@RequestParam Long modelId,
                                                        @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        List<MetaEntityFile> list = entityFileService.listByModelId(modelId);
        return ResultBean.success(list);
    }

    /**
     * 下载实体文件
     */
    @GetMapping("/execute/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                              @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        MetaEntityFile record = entityFileService.getById(fileId);
        if (record == null) return ResponseEntity.notFound().build();
        String filePath = record.getStoragePath();
        if (filePath == null || filePath.isEmpty()) return ResponseEntity.notFound().build();

        File file = new File(filePath);
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        String downloadName = record.getFileName() != null ? record.getFileName() : file.getName();
        String encodedName;
        try {
            encodedName = URLEncoder.encode(downloadName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            encodedName = downloadName;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    /**
     * 删除生成记录及物理文件
     */
    @DeleteMapping("/execute/file/{fileId}")
    public ResultBean<String> deleteFile(@PathVariable Long fileId,
                                          @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        engineService.deleteFile(fileId, empNo);
        return ResultBean.success("删除成功");
    }

    /**
     * 上传文件到 FTP 服务器
     */
    @PostMapping("/execute/upload-ftp")
    public ResultBean<String> uploadToFtp(@RequestBody UploadFtpRequest req,
                                          @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        MetaEntityFile record = entityFileService.getById(req.getFileId());
        if (record == null) return ResultBean.error("文件记录不存在");
        String filePath = record.getStoragePath();
        if (filePath == null || filePath.isEmpty()) return ResultBean.error("文件路径为空");
        File localFile = new File(filePath);
        if (!localFile.exists()) return ResultBean.error("本地文件不存在");

        MetaFtpConfig ftpConfig = ftpConfigService.getById(req.getFtpConfigId());
        if (ftpConfig == null) return ResultBean.error("FTP配置不存在");

        FTPClient ftpClient = new FTPClient();
        try (FileInputStream fis = new FileInputStream(localFile)) {
            int port = ftpConfig.getFtpPort() != null ? ftpConfig.getFtpPort() : 21;
            ftpClient.connect(ftpConfig.getFtpIp(), port);
            if (!ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword())) {
                return ResultBean.error("FTP登录失败，请检查用户名密码");
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setBufferSize(1024 * 1024);

            String remotePath = ftpConfig.getRemotePath();
            if (remotePath != null && !remotePath.isEmpty()) {
                makeDirs(ftpClient, remotePath);
                ftpClient.changeWorkingDirectory(remotePath);
            }

            String remoteFileName = record.getFileName() != null ? record.getFileName() : localFile.getName();
            boolean uploaded = ftpClient.storeFile(remoteFileName, fis);
            if (!uploaded) {
                return ResultBean.error("FTP上传失败: " + ftpClient.getReplyString());
            }
            log.info("FTP upload success: {} -> {}:{}{}/{}", filePath, ftpConfig.getFtpIp(), port, remotePath, remoteFileName);
            return ResultBean.success("上传成功");
        } catch (IOException e) {
            log.error("FTP上传异常", e);
            return ResultBean.error("FTP上传异常: " + e.getMessage());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ignored) {}
        }
    }

    private void makeDirs(FTPClient ftpClient, String path) throws IOException {
        String[] dirs = path.replace('\\', '/').split("/");
        for (String dir : dirs) {
            if (dir.isEmpty()) continue;
            if (!ftpClient.changeWorkingDirectory(dir)) {
                ftpClient.makeDirectory(dir);
                ftpClient.changeWorkingDirectory(dir);
            }
        }
    }

    // ===================== 系统辅助 =====================

    /**
     * 重置序列号
     */
    @PostMapping("/sys/sequence/reset")
    public ResultBean<String> resetSequence(@RequestBody ResetSeqRequest req,
                                             @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        seqService.reset("SEQ", req.getTargetId());
        return ResultBean.success("重置成功");
    }

    /**
     * 手动清理临时文件
     */
    @PostMapping("/sys/clean-temp")
    public ResultBean<String> cleanTemp(@RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        // 简单实现：清理预览目录
        File previewDir = new File("./storage/preview");
        if (previewDir.exists() && previewDir.isDirectory()) {
            File[] files = previewDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
        return ResultBean.success("清理完成");
    }

    // ===================== 工具方法 =====================

    private String getEmpNo(String token) {
        return StringUtils.hasText(token) ? token.trim() : "anonymous";
    }

    // ===================== DTO 内部类 =====================

    public static class GenerateRequest {
        private Long modelId;
        private Integer rowCount;
        private String batchName;

        public Long getModelId() { return modelId; }
        public void setModelId(Long modelId) { this.modelId = modelId; }
        public Integer getRowCount() { return rowCount; }
        public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
        public String getBatchName() { return batchName; }
        public void setBatchName(String batchName) { this.batchName = batchName; }
    }

    public static class ResetSeqRequest {
        private String targetId;
        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
    }

    public static class UploadFtpRequest {
        private Long fileId;
        private Long ftpConfigId;

        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        public Long getFtpConfigId() { return ftpConfigId; }
        public void setFtpConfigId(Long ftpConfigId) { this.ftpConfigId = ftpConfigId; }
    }
}
