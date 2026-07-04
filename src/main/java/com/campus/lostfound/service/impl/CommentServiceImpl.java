package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result listComments(Long itemId, String itemType, int page, int size) {
        if (itemId == null || !StringUtils.hasText(itemType)) {
            return Result.fail(ResultCode.PARAM_ERROR);
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }

        QueryWrapper<Comment> wrapper = new QueryWrapper<>();
        wrapper.eq("item_id", itemId)
                .eq("item_type", itemType)
                .eq("status", Constants.COMMENT_NORMAL)
                .orderByAsc("create_time");

        Page<Comment> pageParam = new Page<>(page, size);
        Page<Comment> result = commentMapper.selectPage(pageParam, wrapper);

        // 批量关联用户信息（username、avatar）
        List<Comment> comments = result.getRecords();
        Map<Long, User> userMap = batchLoadUsers(comments);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Comment comment : comments) {
            records.add(buildCommentVO(comment, userMap));
        }

        // 返回与项目分页约定一致的 IPage 结构（records + total + current + size）
        Page<Map<String, Object>> pageResult = new Page<>(page, size);
        pageResult.setRecords(records);
        pageResult.setTotal(result.getTotal());
        return Result.success(pageResult);
    }

    @Override
    public Result<Boolean> addComment(Long userId, Long itemId, String itemType, String content, Long parentId) {
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        if (itemId == null || !StringUtils.hasText(itemType) || !StringUtils.hasText(content)) {
            return Result.fail(ResultCode.PARAM_ERROR);
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setItemId(itemId);
        comment.setItemType(itemType);
        comment.setContent(escapeXss(content.trim()));
        comment.setParentId(parentId);
        comment.setStatus(Constants.COMMENT_NORMAL);
        commentMapper.insert(comment);
        log.info("用户{}发表评论成功，itemId={}, itemType={}", userId, itemId, itemType);
        return Result.success(true);
    }

    @Override
    public Result<Boolean> deleteComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return Result.fail(ResultCode.PARAM_ERROR);
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            return Result.fail(ResultCode.FORBIDDEN.getCode(), "无权删除他人评论");
        }
        comment.setStatus(Constants.COMMENT_DELETED);
        commentMapper.updateById(comment);
        log.info("用户{}删除评论{}", userId, commentId);
        return Result.success(true);
    }

    /**
     * 批量加载评论发布者用户信息
     */
    private Map<Long, User> batchLoadUsers(List<Comment> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return new HashMap<>();
        }
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    /**
     * 组装评论视图（附加 username、avatar）
     */
    private Map<String, Object> buildCommentVO(Comment comment, Map<Long, User> userMap) {
        Map<String, Object> vo = new HashMap<>(8);
        vo.put("id", comment.getId());
        vo.put("itemId", comment.getItemId());
        vo.put("itemType", comment.getItemType());
        vo.put("userId", comment.getUserId());
        vo.put("parentId", comment.getParentId());
        vo.put("content", comment.getContent());
        vo.put("createTime", comment.getCreateTime());
        User user = userMap.get(comment.getUserId());
        vo.put("username", user != null && StringUtils.hasText(user.getUsername()) ? user.getUsername() : "匿名用户");
        vo.put("avatar", user != null ? user.getAvatar() : null);
        return vo;
    }

    /**
     * XSS 过滤：转义 & < > " '
     * HtmlUtils.htmlEscape 会转义 & < > "，但不会转义单引号，这里额外处理单引号
     */
    private String escapeXss(String content) {
        if (content == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(content).replace("'", "&#39;");
    }
}
