package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.entity.Announcement;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.AnnouncementMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告服务实现类
 */
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<List<Announcement>> getList() {
        QueryWrapper<Announcement> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByDesc("create_time");
        List<Announcement> list = announcementMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<List<Announcement>> getAdminList() {
        QueryWrapper<Announcement> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        List<Announcement> list = announcementMapper.selectList(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<Announcement> getById(Long id) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.success(ann);
    }

    @Override
    public Result create(Long userId, Announcement ann) {
        ann.setPublisherId(userId);
        if (ann.getStatus() == null) {
            ann.setStatus(1);
        }
        announcementMapper.insert(ann);
        return Result.success(ann);
    }

    @Override
    public Result update(Long userId, Announcement ann) {
        User operator = userMapper.selectById(userId);
        if (operator == null || operator.getRole() == null || operator.getRole() != Constants.ROLE_ADMIN) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权操作公告");
        }
        Announcement exist = announcementMapper.selectById(ann.getId());
        if (exist == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        announcementMapper.updateById(ann);
        return Result.success();
    }

    @Override
    public Result delete(Long userId, Long id) {
        User operator = userMapper.selectById(userId);
        if (operator == null || operator.getRole() == null || operator.getRole() != Constants.ROLE_ADMIN) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权操作公告");
        }
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        announcementMapper.deleteById(id);
        return Result.success();
    }
}
