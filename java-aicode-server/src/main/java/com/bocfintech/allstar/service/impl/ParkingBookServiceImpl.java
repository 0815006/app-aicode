package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.ParkingBook;
import com.bocfintech.allstar.mapper.ParkingBookMapper;
import com.bocfintech.allstar.service.ParkingBookService;
import com.bocfintech.allstar.task.ParkBookTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParkingBookServiceImpl extends ServiceImpl<ParkingBookMapper, ParkingBook> implements ParkingBookService {

    @Autowired
    private ParkBookTask parkBookTask; // 假设你有ParkBookTask来处理登录验证

    @Override
    public ParkingBook getMyConfig(String empNo) {
        return this.getById(empNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMyConfig(ParkingBook config) {

        // 验证密码正确性
        if (config.getPassHash() != null && !config.getPassHash().isEmpty()) {
            // 调用登录接口验证密码
            boolean passwordValid = validatePassword(config.getEmpNo(), config.getPassHash());
            if (!passwordValid) {
                return false;
            }
        }

        // saveOrUpdate 会自动判断：如果emp_no存在则UPDATE，不存在则INSERT
        // 确保在保存前不设置 createTime 和 lastTime
        // 这样 MyBatis-Plus 才能正确自动填充

        // 如果你传入的 config 对象中已经设置了 createTime，则需要清空它
//        config.setCreateTime(null); // 在插入时让框架自动填充
//        config.setLastTime(null);   // 在更新时让框架自动填充
        return this.saveOrUpdate(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMyConfig(String empNo) {
        return this.removeById(empNo);
    }


    /**
     * 验证密码正确性
     */
    private boolean validatePassword(String empNo, String passwordHash) {
        try {
            // 调用登录接口验证密码
            // 这里假设你的登录接口返回 boolean 或者其他验证结果
            return parkBookTask.validatePassword(empNo, passwordHash); // 根据实际接口调整
        } catch (Exception e) {
            // 记录日志或处理异常
            log.error("密码验证失败");
            return false;
        }
    }

    @Override
    public List<ParkingBook> getAllRecords() {
        return baseMapper.selectAllInfo();
    }

}
