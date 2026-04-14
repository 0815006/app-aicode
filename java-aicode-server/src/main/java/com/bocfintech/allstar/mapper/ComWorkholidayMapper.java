package com.bocfintech.allstar.mapper;

import com.bocfintech.allstar.entity.ComWorkholiday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

@Mapper
public interface ComWorkholidayMapper extends MyBaseMapper<ComWorkholiday> {
    /**
     * 查询指定日期的工作日状态
     */
    @Select("SELECT status FROM com_workholiday WHERE date = #{date}")
    String getStatusByDate(@Param("date") Date date);

    /**
     * 查询明天的工作日状态
     */
    @Select("SELECT status FROM com_workholiday WHERE date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)")
    String getTomorrowStatus();

}
