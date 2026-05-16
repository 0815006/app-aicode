package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.MetaFtpConfig;

import java.util.List;

public interface MetaFtpConfigService extends IService<MetaFtpConfig> {

    List<MetaFtpConfig> listAll();
}
