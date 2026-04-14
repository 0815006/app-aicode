package com.bocfintech.allstar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bocfintech.allstar.entity.ParkingBook;
import com.bocfintech.allstar.entity.ParkingBookWithRusult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ParkingBookMapper extends BaseMapper<ParkingBook> {

    String sql ="SELECT \n" +
            "    pb.emp_no, \n" +
            "    pb.username, \n" +
            "    pb.auto_book, \n" +
            "    pb.create_time, \n" +
            "    pb.last_time,\n" +
            "    pr.appointment_date AS last_appointment_date,\n" +
            "    pr.result AS last_appointment_result,\n" +
            "    pr.parking_position AS last_parking_position,\n" +
            "    pr.parking_type AS parking_type,\n" +
            "    pr.plate_no AS plate_no \n" +
            "FROM \n" +
            "    parking_book pb\n" +
            "LEFT JOIN (\n" +
            "    SELECT \n" +
            "        emp_no,\n" +
            "        MAX(create_time) AS max_create_time\n" +
            "    FROM \n" +
            "        parking_record\n" +
            "    GROUP BY \n" +
            "        emp_no\n" +
            ") pr_max ON pb.emp_no = pr_max.emp_no\n" +
            "LEFT JOIN parking_record pr ON pr.emp_no = pr_max.emp_no AND pr.create_time = pr_max.max_create_time;";

    // 基础CRUD由BaseMapper提供
//    @Select("SELECT emp_no, username, auto_book, create_time, last_time FROM parking_book ORDER BY create_time DESC")
    @Select(sql)
    List<ParkingBookWithRusult> selectAllInfo();
}
