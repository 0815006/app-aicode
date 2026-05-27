package com.bocfintech.allstar.websocket;

import com.bocfintech.allstar.entity.ChatMessage;
import com.bocfintech.allstar.service.ChatMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/chat/room/{roomId}")
@Component
public class ChatRoomWebSocketServer {

    /** Map<roomId, Map<session, username>> */
    private static final Map<Long, Map<Session, String>> roomSessions = new ConcurrentHashMap<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static ChatMessageService chatMessageService;

    public static void setChatMessageService(ChatMessageService service) {
        chatMessageService = service;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") Long roomId) throws UnsupportedEncodingException {
        String username = "游客" + System.currentTimeMillis() % 10000;
        String query = session.getQueryString();
        if (query != null && query.contains("username=")) {
            String encoded = query.split("username=")[1];
            username = URLDecoder.decode(encoded, "UTF-8");
        }

        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session, username);

        System.out.println("【房间WebSocket】房间" + roomId + " 新连接: " + username + " 当前在线: " + roomSessions.get(roomId).size());

        broadcastRoomOnlineMembers(roomId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") Long roomId) {
        Map<Session, String> roomMap = roomSessions.get(roomId);
        if (roomMap != null) {
            String username = roomMap.remove(session);
            if (roomMap.isEmpty()) {
                roomSessions.remove(roomId);
            }
            System.out.println("【房间WebSocket】房间" + roomId + " 连接断开: " + username + " 剩余: " + (roomMap != null ? roomMap.size() : 0));
        }

        broadcastRoomOnlineMembers(roomId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("roomId") Long roomId) {
        Map<Session, String> roomMap = roomSessions.get(roomId);
        if (roomMap == null) return;

        String username = roomMap.get(session);

        try {
            JsonNode json = objectMapper.readTree(message);
            String type = json.has("type") ? json.get("type").asText() : "chat";

            if ("rename".equals(type)) {
                String oldName = json.get("oldName").asText();
                String newName = json.get("newName").asText();
                roomMap.put(session, newName);

                // 发送改名通知
                Map<String, Object> renameMsg = new HashMap<>();
                renameMsg.put("type", "rename");
                renameMsg.put("oldName", oldName);
                renameMsg.put("newName", newName);
                renameMsg.put("time", System.currentTimeMillis());
                broadcastToRoom(roomId, objectMapper.writeValueAsString(renameMsg));

                broadcastRoomOnlineMembers(roomId);
            } else if ("roomListUpdate".equals(type)) {
                // 房间列表变更通知（加入/退出），广播给所有session
                broadcastRoomOnlineMembers(roomId);
            } else {
                // 保存消息到数据库
                if (chatMessageService != null) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setUsername(username);
                    chatMessage.setMessage(message);
                    chatMessage.setTimestamp(new Date());
                    chatMessage.setRoomId(roomId);
                    chatMessageService.saveRoomMessage(username, message, roomId);
                }

                // 广播给同房间所有session
                broadcastToRoom(roomId, message);
            }
        } catch (Exception e) {
            // 解析失败，当普通消息广播
            broadcastToRoom(roomId, message);
        }
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("roomId") Long roomId) {
        Map<Session, String> roomMap = roomSessions.get(roomId);
        String username = roomMap != null ? roomMap.get(session) : "未知";
        System.err.println("【房间WebSocket错误】房间" + roomId + " 用户: " + username + " " + error.getMessage());
    }

    private void broadcastToRoom(Long roomId, String message) {
        Map<Session, String> roomMap = roomSessions.get(roomId);
        if (roomMap == null) return;

        for (Session s : roomMap.keySet()) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void broadcastRoomOnlineMembers(Long roomId) {
        Map<Session, String> roomMap = roomSessions.get(roomId);
        List<String> onlineUsernames = roomMap != null
                ? new ArrayList<>(roomMap.values())
                : new ArrayList<>();

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "roomOnlineMembers");
        msg.put("roomId", roomId);
        msg.put("users", onlineUsernames);
        msg.put("time", System.currentTimeMillis());

        try {
            broadcastToRoom(roomId, objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
