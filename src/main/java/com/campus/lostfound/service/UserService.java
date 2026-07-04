package com.campus.lostfound.service;

import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    Result register(RegisterDTO dto);

    /**
     * 用户登录
     */
    Result login(LoginDTO dto);

    /**
     * 退出登录
     */
    Result logout(Long userId);

    /**
     * 获取用户信息
     */
    Result<UserVO> getUserInfo(Long userId);

    /**
     * 更新个人资料
     */
    Result updateProfile(Long userId, RegisterDTO dto);

    /**
     * 更新头像
     */
    Result updateAvatar(Long userId, String avatar);

    /**
     * 用户列表（管理员）
     */
    Result<List<UserVO>> getUserList(String keyword);

    /**
     * 更新用户状态（管理员）
     */
    Result updateUserStatus(Long userId, Integer status);
}
