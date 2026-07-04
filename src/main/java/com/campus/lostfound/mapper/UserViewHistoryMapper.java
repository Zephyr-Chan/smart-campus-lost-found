package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.UserViewHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户浏览历史Mapper
 */
@Mapper
public interface UserViewHistoryMapper extends BaseMapper<UserViewHistory> {
}
