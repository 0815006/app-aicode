package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.MetaFtpConfig;
import com.bocfintech.allstar.mapper.MetaFtpConfigMapper;
import com.bocfintech.allstar.service.MetaFtpConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MetaFtpConfigServiceImpl
        extends ServiceImpl<MetaFtpConfigMapper, MetaFtpConfig>
        implements MetaFtpConfigService {

    @Override
    public List<MetaFtpConfig> listAll() {
        return lambdaQuery().orderByDesc(MetaFtpConfig::getUpdateTime).list();
    }
}
