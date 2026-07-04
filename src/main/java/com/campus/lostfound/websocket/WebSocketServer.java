package com.campus.lostfound.websocket;

import com.campus.lostfound.common.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务端
 */
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    /** 在线用户Session映射 */
    private static final ConcurrentHashMap<Long, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /** JwtUtil通过Spring注入（static字段需在onOpen中通过ApplicationContext获取） */
    private static JwtUtil jwtUtil;

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        WebSocketServer.jwtUtil = jwtUtil;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        // H5修复：WebSocket连接认证 - 校验token参数
        String token = getQueryParam(session, "token");
        if (token == null || token.isEmpty()) {
            logger.warn("WebSocket连接拒绝：未携带token, userId={}", userId);
            closeSession(session);
            return;
        }
        if (jwtUtil == null || !jwtUtil.validateToken(token)) {
            logger.warn("WebSocket连接拒绝：token无效, userId={}", userId);
            closeSession(session);
            return;
        }
        Long tokenUserId = jwtUtil.getUserIdFromToken(token);
        if (!userId.equals(tokenUserId)) {
            logger.warn("WebSocket连接拒绝：userId不匹配, pathUserId={}, tokenUserId={}", userId, tokenUserId);
            closeSession(session);
            return;
        }

        SESSION_MAP.put(userId, session);
        logger.info("WebSocket连接建立, userId={}, 当前在线人数={}", userId, SESSION_MAP.size());
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        SESSION_MAP.remove(userId);
        logger.info("WebSocket连接关闭, userId={}, 当前在线人数={}", userId, SESSION_MAP.size());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("收到WebSocket消息: {}", message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("WebSocket发生错误", error);
    }

    /**
     * 发送消息给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public static void sendMessage(Long userId, String message) {
        Session session = SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                logger.info("WebSocket消息已发送, userId={}", userId);
            } catch (IOException e) {
                logger.error("WebSocket消息发送失败, userId={}", userId, e);
            }
        }
    }

    /**
     * 获取当前在线人数
     */
    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }

    /**
     * 从Session的查询字符串中提取参数值
     */
    private String getQueryParam(Session session, String key) {
        String queryString = session.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }
        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    /**
     * 关闭Session
     */
    private void closeSession(Session session) {
        try {
            session.close();
        } catch (IOException e) {
            logger.error("关闭WebSocket Session失败", e);
        }
    }
}
