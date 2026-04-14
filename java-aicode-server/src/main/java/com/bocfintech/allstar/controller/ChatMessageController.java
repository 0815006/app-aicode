package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.ChatMessage;
import com.bocfintech.allstar.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {

    @Autowired
    private ChatMessageService chatMessageService;

    /**
     * 获取某时间戳之前的聊天记录
     */
    @GetMapping("/history")
    public ResultBean<List<ChatMessage>> getChatHistory(
            @RequestParam Long timestamp,
            @RequestParam int limit) {
        return chatMessageService.getMessagesBeforeTimestamp(timestamp, limit);
    }

    /**
     * 获取当天的聊天记录
     */
    @GetMapping("/today")
    public ResultBean<List<ChatMessage>> getTodayMessages() {
        return chatMessageService.getTodayMessages();
    }

    /**
     * 保存聊天消息（由 WebSocket 调用）
     */
    @PostMapping("/save")
    public ResultBean<String> saveMessage(@RequestBody ChatMessage chatMessage) {
        if (StringUtils.isEmpty(chatMessage.getUsername()) || StringUtils.isEmpty(chatMessage.getMessage())) {
            return ResultBean.error(ErrorEnum.参数异常, "用户名或消息不能为空");
        }
        return chatMessageService.saveChatMessage(chatMessage.getUsername(), chatMessage.getMessage());
    }
}
