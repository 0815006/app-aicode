package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.MetaRefFile;
import com.bocfintech.allstar.mapper.MetaRefFileMapper;
import com.bocfintech.allstar.service.MetaRefFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MetaRefFileServiceImpl
        extends ServiceImpl<MetaRefFileMapper, MetaRefFile>
        implements MetaRefFileService {

    @Override
    public List<MetaRefFile> listAll() {
        return lambdaQuery().orderByAsc(MetaRefFile::getRefName).list();
    }

    @Override
    public MetaRefFile uploadAndSave(String refName, String filePath, String parseType, String delimiter, String columnMapping) {
        MetaRefFile refFile = new MetaRefFile();
        refFile.setRefName(refName);
        refFile.setFilePath(filePath);
        refFile.setParseType(parseType);
        refFile.setDelimiter(delimiter);
        refFile.setColumnMapping(columnMapping);
        save(refFile);
        return refFile;
    }
}
