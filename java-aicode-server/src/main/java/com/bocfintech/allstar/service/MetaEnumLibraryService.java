package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.MetaEnumLibrary;

import java.util.List;

public interface MetaEnumLibraryService extends IService<MetaEnumLibrary> {

    List<MetaEnumLibrary> listAll();

    List<String> getAllKeys();
}
