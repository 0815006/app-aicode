package com.bocfintech.allstar.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.constants.ErrorEnum;
import com.bocfintech.allstar.entity.ChatRoom;
import com.bocfintech.allstar.entity.ChatRoomMember;
import com.bocfintech.allstar.mapper.ChatRoomMapper;
import com.bocfintech.allstar.mapper.ChatRoomMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomMapper chatRoomMapper;

    @Autowired
    private ChatRoomMemberMapper chatRoomMemberMapper;

    /** 创建房间，创建者自动加入 */
    @Transactional
    public ResultBean<ChatRoom> createRoom(String title, String creator) {
        // 检查同名房间
        QueryWrapper<ChatRoom> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("title", title).eq("status", 0);
        if (chatRoomMapper.selectCount(checkWrapper) > 0) {
            return ResultBean.error(ErrorEnum.参数异常, "房间名已存在");
        }

        ChatRoom room = new ChatRoom();
        room.setTitle(title);
        room.setCreator(creator);
        room.setStatus(0);
        room.setCreatedAt(new Date());
        room.setUpdatedAt(new Date());
        chatRoomMapper.insert(room);

        // 创建者自动加入
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(room.getId());
        member.setUsername(creator);
        member.setJoinedAt(new Date());
        chatRoomMemberMapper.insert(member);

        return ResultBean.success(room);
    }

    /** 获取房间列表（含成员），排序：我在的房间排前面，再按创建时间倒序 */
    public ResultBean<List<Map<String, Object>>> getRoomList(String currentUsername) {
        // 所有活跃房间
        QueryWrapper<ChatRoom> roomWrapper = new QueryWrapper<>();
        roomWrapper.eq("status", 0).orderByDesc("created_at");
        List<ChatRoom> rooms = chatRoomMapper.selectList(roomWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatRoom room : rooms) {
            Map<String, Object> roomMap = new HashMap<>();
            roomMap.put("id", room.getId());
            roomMap.put("title", room.getTitle());
            roomMap.put("creator", room.getCreator());
            roomMap.put("createdAt", room.getCreatedAt());

            // 获取成员列表
            QueryWrapper<ChatRoomMember> memberWrapper = new QueryWrapper<>();
            memberWrapper.eq("room_id", room.getId());
            List<ChatRoomMember> members = chatRoomMemberMapper.selectList(memberWrapper);
            List<String> memberNames = members.stream()
                    .map(ChatRoomMember::getUsername)
                    .collect(Collectors.toList());
            roomMap.put("members", memberNames);
            roomMap.put("memberCount", memberNames.size());

            // 标记当前用户是否在房间中
            boolean iAmIn = memberNames.contains(currentUsername);
            roomMap.put("iAmIn", iAmIn);

            result.add(roomMap);
        }

        // 排序：我在的房间在前面，同组按创建时间倒序（已经是倒序）
        result.sort((a, b) -> {
            boolean aIn = Boolean.TRUE.equals(a.get("iAmIn"));
            boolean bIn = Boolean.TRUE.equals(b.get("iAmIn"));
            if (aIn && !bIn) return -1;
            if (!aIn && bIn) return 1;
            // 同组保持原顺序（创建时间倒序）
            return 0;
        });

        return ResultBean.success(result);
    }

    /** 加入房间 */
    @Transactional
    public ResultBean<String> joinRoom(Long roomId, String username) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null || room.getStatus() == 1) {
            return ResultBean.error(ErrorEnum.参数异常, "房间不存在或已销毁");
        }

        // 检查是否已在房间中
        QueryWrapper<ChatRoomMember> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId).eq("username", username);
        if (chatRoomMemberMapper.selectCount(wrapper) > 0) {
            return ResultBean.success("已在房间中");
        }

        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUsername(username);
        member.setJoinedAt(new Date());
        chatRoomMemberMapper.insert(member);
        return ResultBean.success("加入成功");
    }

    /** 退出房间，如果最后一人退出则自动销毁房间 */
    @Transactional
    public ResultBean<String> leaveRoom(Long roomId, String username) {
        // 删除成员
        QueryWrapper<ChatRoomMember> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId).eq("username", username);
        chatRoomMemberMapper.delete(wrapper);

        // 检查剩余成员数
        QueryWrapper<ChatRoomMember> countWrapper = new QueryWrapper<>();
        countWrapper.eq("room_id", roomId);
        Long remaining = chatRoomMemberMapper.selectCount(countWrapper);

        if (remaining == 0) {
            // 空房间自动销毁（逻辑删除）
            ChatRoom room = chatRoomMapper.selectById(roomId);
            if (room != null) {
                room.setStatus(1);
                room.setUpdatedAt(new Date());
                chatRoomMapper.updateById(room);
                return ResultBean.success("已退出，房间已自动销毁");
            }
        }
        return ResultBean.success("已退出房间");
    }

    /** 获取房间成员 */
    public ResultBean<List<String>> getRoomMembers(Long roomId) {
        QueryWrapper<ChatRoomMember> wrapper = new QueryWrapper<>();
        wrapper.eq("room_id", roomId);
        List<ChatRoomMember> members = chatRoomMemberMapper.selectList(wrapper);
        List<String> names = members.stream()
                .map(ChatRoomMember::getUsername)
                .collect(Collectors.toList());
        return ResultBean.success(names);
    }

    /** 获取用户所在的所有房间ID列表（用于改名后的自动匹配/刷新） */
    public ResultBean<List<Long>> getUserRoomIds(String username) {
        QueryWrapper<ChatRoomMember> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        List<ChatRoomMember> members = chatRoomMemberMapper.selectList(wrapper);
        List<Long> roomIds = members.stream()
                .map(ChatRoomMember::getRoomId)
                .collect(Collectors.toList());
        return ResultBean.success(roomIds);
    }
}
