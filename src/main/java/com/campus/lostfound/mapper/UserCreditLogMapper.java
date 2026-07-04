package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.UserCreditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信用积分日志Mapper
 */
@Mapper
public interface UserCreditLogMapper extends BaseMapper<UserCreditLog> {
}
