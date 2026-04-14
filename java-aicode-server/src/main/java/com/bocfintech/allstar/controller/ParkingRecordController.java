package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.ParkingRecord;
import com.bocfintech.allstar.service.ParkingRecordService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking/record")
public class ParkingRecordController {

    @Autowired
    private ParkingRecordService parkingRecordService;

    /**
     * 1. 新增预约记录 (单条)
     */
    @PostMapping("/add")
    public ResultBean addRecord(@RequestBody ParkingRecord record) {
        // 可选：如果业务要求，也可以在这里校验车牌、车位等格式
        boolean success = parkingRecordService.addRecord(record);
        return success ? ResultBean.success("预约成功") : ResultBean.error(ErrorEnum.操作失败, "预约失败");
    }

    /**
     * 2. 查询本人预约列表 (分页)
     * 参照 queryUploadList 风格
     */
    @GetMapping("/queryList")
    public ResultBean queryList(@RequestParam(value = "appointmentDate", required = false) String appointmentDate, // 可选：按日期筛选
                                @RequestParam(value = "result", required = false) String result,                  // 可选：按结果筛选 (成功/取消)
                                @RequestParam(value = "empId", required = true) String empId,
            @RequestParam(value = "pageNum", required = true, defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", required = true, defaultValue = "10") int pageSize
    ) {
        // 构建查询条件
        LambdaQueryWrapper<ParkingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ParkingRecord::getEmpNo, empId); // 核心：只查本人的

        if (StringUtils.hasText(appointmentDate)) {
            wrapper.eq(ParkingRecord::getAppointmentDate, appointmentDate);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(ParkingRecord::getResult, result);
        }

        // 排序：按预约日期倒序，再按创建时间倒序
        wrapper.orderByDesc(ParkingRecord::getAppointmentDate)
                .orderByDesc(ParkingRecord::getCreateTime);

        // 分页查询
        PageHelper.startPage(pageNum, pageSize);
        List<ParkingRecord> list = parkingRecordService.list(wrapper);

        if (list == null || list.isEmpty()) {
            return ResultBean.success(new PageInfo<>(list));
        }

        PageInfo<ParkingRecord> pageInfo = new PageInfo<>(list);
        return ResultBean.success(pageInfo);
    }

}
