package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

/**
 * 站内消息服务接口
 * 提供消息列表查询、未读计数、标记已读、删除及消息发送能力
 */
public interface MessageService {

    /**
     * 分页查询当前用户的消息列表
     *
     * @param userId 用户ID
     * @param page   页码（从1开始）
     * @param size   每页条数
     * @param isRead 已读筛选（0-未读，1-已读，null-全部）
     * @return 分页消息列表
     */
    Result listMessages(Long userId, int page, int size, Integer isRead);

    /**
     * 获取当前用户的未读消息数（优先读Redis缓存）
     *
     * @param userId 用户ID
     * @return 未读消息数
     */
    Result<Integer> getUnreadCount(Long userId);

    /**
     * 将指定消息标记为已读
     *
     * @param messageId 消息ID
     * @param userId    用户ID（用于权限校验）
     * @return 是否操作成功
     */
    Result<Boolean> markAsRead(Long messageId, Long userId);

    /**
     * 将当前用户所有未读消息标记为已读
     *
     * @param userId 用户ID
     * @return 是否操作成功
     */
    Result<Boolean> markAllAsRead(Long userId);

    /**
     * 删除指定消息（校验接收者归属后删除）
     *
     * @param messageId 消息ID
     * @param userId    用户ID（用于权限校验）
     * @return 是否操作成功
     */
    Result<Boolean> deleteMessage(Long messageId, Long userId);

    /**
     * 发送一条站内消息，并尝试通过WebSocket实时推送
     *
     * @param receiverId 接收者ID
     * @param senderId   发送者ID（系统消息为0或null）
     * @param type       消息类型（match/claim/credit/system）
     * @param title      消息标题
     * @param content    消息内容
     * @param refId      关联业务ID
     */
    void sendMessage(Long receiverId, Long senderId, String type, String title, String content, Long refId);
}
