package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.ChatMessage;
import com.bocfintech.allstar.mapper.ChatMessageMapper;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /**
     * 保存聊天消息
     */
    public ResultBean<String> saveChatMessage(String username, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUsername(username);
        chatMessage.setMessage(message);
        chatMessage.setTimestamp(new Date());

        boolean result = chatMessageMapper.insert(chatMessage) > 0;
        return result ? ResultBean.success("消息已保存") : ResultBean.error(ErrorEnum.操作失败, "消息保存失败");
    }

    /**
     * 获取某时间戳之前的聊天记录（降序排列，限制 20 条）
     */
    public ResultBean<List<ChatMessage>> getMessagesBeforeTimestamp(Long timestamp, int limit) {
        Date targetTime = new Date(timestamp);
        List<ChatMessage> messages = chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
                .lt("timestamp", targetTime)
                .orderByDesc("timestamp")
                .last("LIMIT " + limit));

        return ResultBean.success(messages);
    }

    /**
     * 获取当天的聊天记录
     */
    public ResultBean<List<ChatMessage>> getTodayMessages() {
        Date today = new Date(); // 当前时间
        List<ChatMessage> messages = chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
                .ge("timestamp", DateUtils.truncate(today, Calendar.DAY_OF_MONTH))
                .lt("timestamp", DateUtils.addDays(DateUtils.truncate(today, Calendar.DAY_OF_MONTH), 1))
                .orderByDesc("timestamp"));

        return ResultBean.success(messages);
    }
}
