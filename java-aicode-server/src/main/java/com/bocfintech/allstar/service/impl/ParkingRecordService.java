package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.ParkingRecord;

public interface ParkingRecordService extends IService<ParkingRecord> {
    
    /**
     * 新增单条记录
     */
    boolean addRecord(ParkingRecord record);

    /**
     * 分页查询某人的记录列表
     */
    Page<ParkingRecord> listByEmpNo(String empNo, int pageNum, int pageSize);
}
