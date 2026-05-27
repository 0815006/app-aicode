package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.ChatMessage;
import com.bocfintech.allstar.entity.ChatRoom;
import com.bocfintech.allstar.service.ChatMessageService;
import com.bocfintech.allstar.service.ChatRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/room")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatMessageService chatMessageService;

    /** 获取房间列表（含成员信息、排序） */
    @GetMapping("/list")
    public ResultBean<List<Map<String, Object>>> getRoomList(@RequestParam(required = false) String username) {
        return chatRoomService.getRoomList(username);
    }

    /** 创建房间 */
    @PostMapping("/create")
    public ResultBean<ChatRoom> createRoom(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String creator = body.get("creator");
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(creator)) {
            return ResultBean.error(ErrorEnum.参数异常, "标题和创建者不能为空");
        }
        return chatRoomService.createRoom(title, creator);
    }

    /** 加入房间 */
    @PostMapping("/join")
    public ResultBean<String> joinRoom(@RequestBody Map<String, String> body) {
        Long roomId = Long.valueOf(body.get("roomId"));
        String username = body.get("username");
        return chatRoomService.joinRoom(roomId, username);
    }

    /** 退出房间 */
    @PostMapping("/leave")
    public ResultBean<String> leaveRoom(@RequestBody Map<String, String> body) {
        Long roomId = Long.valueOf(body.get("roomId"));
        String username = body.get("username");
        return chatRoomService.leaveRoom(roomId, username);
    }

    /** 获取房间成员 */
    @GetMapping("/{roomId}/members")
    public ResultBean<List<String>> getRoomMembers(@PathVariable Long roomId) {
        return chatRoomService.getRoomMembers(roomId);
    }

    /** 获取房间历史消息 */
    @GetMapping("/{roomId}/history")
    public ResultBean<List<ChatMessage>> getRoomHistory(
            @PathVariable Long roomId,
            @RequestParam Long timestamp,
            @RequestParam int limit) {
        return chatMessageService.getRoomMessagesBeforeTimestamp(roomId, timestamp, limit);
    }

    /** 获取用户所在的所有房间ID列表 */
    @GetMapping("/user-rooms")
    public ResultBean<List<Long>> getUserRoomIds(@RequestParam String username) {
        return chatRoomService.getUserRoomIds(username);
    }

    /** 保存房间消息 */
    @PostMapping("/message")
    public ResultBean<String> saveRoomMessage(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String message = (String) body.get("message");
        Long roomId = body.get("roomId") != null ? Long.valueOf(body.get("roomId").toString()) : null;
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(message)) {
            return ResultBean.error(ErrorEnum.参数异常, "用户名或消息不能为空");
        }
        return chatMessageService.saveRoomMessage(username, message, roomId);
    }
}
