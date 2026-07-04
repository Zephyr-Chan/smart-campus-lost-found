package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.entity.Announcement;

import java.util.List;

/**
 * 公告服务接口
 */
public interface AnnouncementService {

    /**
     * 公告列表（只返回已发布的）
     */
    Result<List<Announcement>> getList();

    /**
     * 公告列表（管理员，返回全部状态）
     */
    Result<List<Announcement>> getAdminList();

    /**
     * 公告详情
     */
    Result<Announcement> getById(Long id);

    /**
     * 创建公告
     */
    Result create(Long userId, Announcement ann);

    /**
     * 更新公告
     */
    Result update(Long userId, Announcement ann);

    /**
     * 删除公告
     */
    Result delete(Long userId, Long id);
}
