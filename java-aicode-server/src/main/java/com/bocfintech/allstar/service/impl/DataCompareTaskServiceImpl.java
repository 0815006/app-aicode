package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.DataCompareResult;
import com.bocfintech.allstar.entity.DataCompareTask;
import com.bocfintech.allstar.mapper.DataCompareResultMapper;
import com.bocfintech.allstar.mapper.DataCompareTaskMapper;
import com.bocfintech.allstar.service.DataCompareTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataCompareTaskServiceImpl
        extends ServiceImpl<DataCompareTaskMapper, DataCompareTask>
        implements DataCompareTaskService {

    @Autowired
    private DataCompareResultMapper dataCompareResultMapper;

    @Override
    public List<DataCompareTask> listByUser(String createdBy) {
        return lambdaQuery()
                .eq(DataCompareTask::getCreatedBy, createdBy)
                .orderByDesc(DataCompareTask::getCreatedTime)
                .list();
    }

    @Override
    @Transactional
    public DataCompareTask createCompareTask(DataCompareTask task, String operator) {
        task.setCreatedBy(operator);
        task.setStatus("SUCCESS");
        task.setCreatedTime(LocalDateTime.now());
        save(task);

        if (task.getResults() != null && !task.getResults().isEmpty()) {
            for (DataCompareResult result : task.getResults()) {
                result.setTaskId(task.getId());
                dataCompareResultMapper.insert(result);
            }
        }
        return task;
    }
}
