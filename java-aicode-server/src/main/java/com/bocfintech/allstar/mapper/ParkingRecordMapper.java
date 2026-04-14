package com.bocfintech.allstar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bocfintech.allstar.entity.ParkingRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingRecordMapper extends BaseMapper<ParkingRecord> {
    // 基础CRUD由BaseMapper提供
}
