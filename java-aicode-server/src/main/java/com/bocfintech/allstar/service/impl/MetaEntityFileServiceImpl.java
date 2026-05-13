package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.MetaEntityFile;
import com.bocfintech.allstar.mapper.MetaEntityFileMapper;
import com.bocfintech.allstar.service.MetaEntityFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MetaEntityFileServiceImpl
        extends ServiceImpl<MetaEntityFileMapper, MetaEntityFile>
        implements MetaEntityFileService {

    @Override
    public List<MetaEntityFile> listByModelId(Long modelId) {
        return lambdaQuery()
                .eq(MetaEntityFile::getModelId, modelId)
                .orderByDesc(MetaEntityFile::getCreateTime)
                .list();
    }

    @Override
    public MetaEntityFile createRecord(Long modelId, String fileName, String fileType, String createUser) {
        MetaEntityFile entityFile = new MetaEntityFile();
        entityFile.setModelId(modelId);
        entityFile.setFileName(fileName);
        entityFile.setFileType(fileType);
        entityFile.setStatus("RUNNING");
        entityFile.setCreateUser(createUser);
        entityFile.setCreateTime(LocalDateTime.now());
        entityFile.setRowCount(0);
        entityFile.setStoragePath(""); // 初始设置为空字符串，避免数据库非空约束报错
        save(entityFile);
        return entityFile;
    }
}
