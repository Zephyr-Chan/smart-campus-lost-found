package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private RedisUtil redisUtil;

    /** 未读计数缓存过期时间（分钟），与登录会话生命周期相近 */
    private static final long UNREAD_TTL_MINUTES = 1440;

    @Override
    public Result listMessages(Long userId, int page, int size, Integer isRead) {
        Page<Message> pageParam = new Page<>(page, size);
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", userId);
        if (isRead != null) {
            wrapper.eq("is_read", isRead);
        }
        wrapper.orderByDesc("create_time");
        Page<Message> result = messageMapper.selectPage(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>(8);
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        data.put("totalPages", result.getPages());
        return Result.success(data);
    }

    @Override
    public Result<Integer> getUnreadCount(Long userId) {
        String unreadKey = Constants.UNREAD_PREFIX + userId;
        // 优先读取Redis缓存
        String cached = redisUtil.get(unreadKey);
        if (cached != null) {
            try {
                return Result.success(Integer.parseInt(cached));
            } catch (NumberFormatException e) {
                log.warn("未读计数缓存格式异常, key={}, value={}", unreadKey, cached);
            }
        }
        // 缓存未命中，查询数据库
        QueryWrapper<Message> wrapper = new QueryWrapper<>();
        wrapper.eq("receiver_id", userId).eq("is_read", 0);
        Long count = messageMapper.selectCount(wrapper);
        int unread = count != null ? count.intValue() : 0;
        // 回写缓存
        redisUtil.set(unreadKey, unread, UNREAD_TTL_MINUTES);
        return Result.success(unread);
    }

    @Override
    public Result<Boolean> markAsRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "消息不存在");
        }
        // 校验接收者归属
        if (!message.getReceiverId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权操作此消息");
        }
        // 已读则无需重复操作
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return Result.success(true);
        }
        message.setIsRead(1);
        messageMapper.updateById(message);

        // 递减Redis未读计数
        decrementUnreadCount(userId);
        return Result.success(true);
    }

    @Override
    public Result<Boolean> markAllAsRead(Long userId) {
        UpdateWrapper<Message> wrapper = new UpdateWrapper<>();
        wrapper.eq("receiver_id", userId).eq("is_read", 0).set("is_read", 1);
        messageMapper.update(null, wrapper);

        // 清空Redis未读计数缓存
        redisUtil.delete(Constants.UNREAD_PREFIX + userId);
        return Result.success(true);
    }

    @Override
    public Result<Boolean> deleteMessage(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "消息不存在");
        }
        // 校验接收者归属
        if (!message.getReceiverId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权删除此消息");
        }
        messageMapper.deleteById(messageId);

        // 若删除的是未读消息，同步递减缓存计数
        if (message.getIsRead() != null && message.getIsRead() == 0) {
            decrementUnreadCount(userId);
        }
        return Result.success(true);
    }

    @Override
    public void sendMessage(Long receiverId, Long senderId, String type, String title, String content, Long refId) {
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setSenderId(senderId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setRefId(refId);
        message.setIsRead(0);
        messageMapper.insert(message);

        // Redis可用时递增未读计数
        if (redisUtil.isAvailable()) {
            try {
                redisUtil.incr(Constants.UNREAD_PREFIX + receiverId);
            } catch (Exception e) {
                log.warn("递增未读计数失败, receiverId={}, 错误: {}", receiverId, e.getMessage());
            }
        }

        // 尝试WebSocket实时推送
        try {
            String wsMessage = String.format(
                    "{\"type\":\"%s\",\"action\":\"new\",\"messageId\":%d,\"title\":\"%s\",\"refId\":%s}",
                    type, message.getId(), escapeJson(title), refId == null ? "null" : refId.toString());
            WebSocketServer.sendMessage(receiverId, wsMessage);
        } catch (Exception e) {
            log.warn("WebSocket消息推送失败, receiverId={}, 错误: {}", receiverId, e.getMessage());
        }
    }

    /**
     * 递减Redis中缓存的未读计数
     */
    private void decrementUnreadCount(Long userId) {
        String unreadKey = Constants.UNREAD_PREFIX + userId;
        String cached = redisUtil.get(unreadKey);
        if (cached == null) {
            // 缓存不存在则删除，下次查询时重新从DB加载
            return;
        }
        try {
            int count = Math.max(0, Integer.parseInt(cached) - 1);
            redisUtil.set(unreadKey, count, UNREAD_TTL_MINUTES);
        } catch (NumberFormatException e) {
            redisUtil.delete(unreadKey);
        }
    }

    /**
     * 简单JSON字符串转义
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
