package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;

/**
 * 评论服务接口
 */
public interface CommentService {

    /**
     * 分页查询某物品的评论列表（含发布者用户信息）
     *
     * @param itemId   物品ID
     * @param itemType 物品类型（lost/found）
     * @param page     页码
     * @param size     每页数量
     * @return 分页评论数据
     */
    Result listComments(Long itemId, String itemType, int page, int size);

    /**
     * 发表评论
     *
     * @param userId    用户ID
     * @param itemId    物品ID
     * @param itemType  物品类型（lost/found）
     * @param content   评论内容（会被XSS过滤）
     * @param parentId  父评论ID（可为空）
     * @return 是否成功
     */
    Result<Boolean> addComment(Long userId, Long itemId, String itemType, String content, Long parentId);

    /**
     * 删除评论（软删除，仅作者可删）
     *
     * @param commentId 评论ID
     * @param userId    当前用户ID
     * @return 是否成功
     */
    Result<Boolean> deleteComment(Long commentId, Long userId);
}
