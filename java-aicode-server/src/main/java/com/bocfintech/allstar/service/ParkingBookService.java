package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.ParkingBook;

import java.util.List;

public interface ParkingBookService extends IService<ParkingBook> {
    
    /**
     * 查询本人配置
     */
    ParkingBook getMyConfig(String empNo);

    /**
     * 更新或插入本人配置 (Upsert)
     */
    boolean updateMyConfig(ParkingBook config);

    /**
     * 删除本人配置
     */
    boolean deleteMyConfig(String empNo);

    /**
     * 查所有配置
     */
    List<ParkingBook> getAllRecords();

}
