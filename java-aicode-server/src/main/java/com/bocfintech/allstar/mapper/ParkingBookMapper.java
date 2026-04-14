package com.bocfintech.allstar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bocfintech.allstar.entity.ParkingBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ParkingBookMapper extends BaseMapper<ParkingBook> {
    // 基础CRUD由BaseMapper提供
    @Select("SELECT emp_no, username, auto_book, create_time, last_time FROM parking_book ORDER BY create_time DESC")
    List<ParkingBook> selectAllInfo();
}
