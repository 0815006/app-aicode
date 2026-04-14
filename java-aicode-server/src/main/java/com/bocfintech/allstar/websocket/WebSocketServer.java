package com.bocfintech.allstar.websocket;

import com.bocfintech.allstar.entity.ChatMessage;
import com.bocfintech.allstar.service.ChatMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint("/ws/chat")
@Component
public class WebSocketServer {

    private static final AtomicInteger connectionCounter = new AtomicInteger(0);
    private static final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();

    // 🔥 新增：用 Map 记录 session -> username
    private static final Map<Session, String> sessionUsernameMap = new HashMap<>();

    // 使用 MyBatis Plus 服务
    private static ChatMessageService chatMessageService;

    // 设置 Spring 注入
    public static void setChatMessageService(ChatMessageService service) {
        chatMessageService = service;
    }

    // 🔥 新增：ObjectMapper 用于生成 JSON
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) throws UnsupportedEncodingException {
        // 从查询参数获取用户名，例如：/ws/chat?username=张三
        String query = session.getQueryString();
        String username = "游客" + connectionCounter.incrementAndGet();

        if (query != null && query.contains("username=")) {
            // 提取 username 值
            String encodedUsername = query.split("username=")[1];
            // ✅ URL 解码
            username = URLDecoder.decode(encodedUsername, "UTF-8");
        }

        sessions.add(session);
        sessionUsernameMap.put(session, username);

        int id = connectionCounter.incrementAndGet();
        System.out.println("新连接：" + session.getId() + ", 用户名：" + username + ", 当前总数：" + sessions.size());

        // ✅ 广播：更新在线用户列表
        broadcastOnlineUsers();
    }

    @OnClose
    public void onClose(Session session) {
        String username = sessionUsernameMap.get(session);
        sessions.remove(session);
        sessionUsernameMap.remove(session);

        System.out.println("连接关闭：" + session.getId() + ", 用户名：" + username + ", 剩余：" + sessions.size());

        // ✅ 广播：更新在线用户列表
        broadcastOnlineUsers();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("【收到消息】来自 " + session.getId() + "：" + message);

        try {
            // 解析 JSON
            JsonNode json = objectMapper.readTree(message);
            String type = json.has("type") ? json.get("type").asText() : "chat";

            if ("rename".equals(type)) {
                String oldName = json.get("oldName").asText();
                String newName = json.get("newName").asText();

                // 更新用户名映射
                sessionUsernameMap.put(session, newName);
                System.out.println("用户改名：" + oldName + " → " + newName);

                // 广播最新的在线列表
                broadcastOnlineUsers();

            } else {
                // 保存消息到数据库
                String username = sessionUsernameMap.get(session);
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setUsername(username);
                chatMessage.setMessage(message);
                chatMessage.setTimestamp(new Date());

                chatMessageService.saveChatMessage(chatMessage.getUsername(), chatMessage.getMessage());


                // 普通聊天消息，直接广播
                for (Session s : sessions) {
                    if (s.isOpen()) {
                        try {
                            s.getBasicRemote().sendText(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败 → 当普通消息广播
            for (Session s : sessions) {
                if (s.isOpen()) {
                    try {
                        s.getBasicRemote().sendText(message);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }


    @OnError
    public void onError(Session session, Throwable error) {
        String username = sessionUsernameMap.get(session);
        System.err.println("WebSocket 错误：" + error.getMessage() + ", 用户：" + username);
        error.printStackTrace();
    }

    // 🔥 新增：广播当前在线用户列表
    private void broadcastOnlineUsers() {
        // 获取当前所有在线用户名
        List<String> onlineUsernames = new ArrayList<>(sessionUsernameMap.values());

        // 构造消息：{ "type": "onlineUsers", "users": ["张三", "李四"] }
        Map<String, Object> message = new HashMap<>();
        message.put("type", "onlineUsers");
        message.put("users", onlineUsernames);
        message.put("time", System.currentTimeMillis());

        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        // 广播给所有客户端
        for (Session s : sessions) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(jsonMessage);
                    System.out.println("【广播在线列表】" + jsonMessage + " → " + s.getId());
                } catch (IOException e) {
                    System.err.println("发送在线列表失败：" + s.getId());
                    e.printStackTrace();
                }
            }
        }
    }
}
