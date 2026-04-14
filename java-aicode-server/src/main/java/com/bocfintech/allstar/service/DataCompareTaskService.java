package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bocfintech.allstar.entity.DataCompareTask;
import java.util.List;

public interface DataCompareTaskService extends IService<DataCompareTask> {
    List<DataCompareTask> listByUser(String createdBy);
    DataCompareTask createCompareTask(DataCompareTask task, String operator);
}
