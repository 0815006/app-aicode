package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.ParkingRecord;
import com.bocfintech.allstar.mapper.ParkingRecordMapper;
import com.bocfintech.allstar.service.ParkingRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingRecordServiceImpl extends ServiceImpl<ParkingRecordMapper, ParkingRecord> implements ParkingRecordService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addRecord(ParkingRecord record) {
        return this.save(record);
    }

    @Override
    public Page<ParkingRecord> listByEmpNo(String empNo, int pageNum, int pageSize) {
        LambdaQueryWrapper<ParkingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ParkingRecord::getEmpNo, empNo)
               .orderByDesc(ParkingRecord::getCreateTime); // 按创建时间倒序

        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }
}
