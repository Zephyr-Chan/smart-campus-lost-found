package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.JwtUtil;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.service.UserService;
import com.campus.lostfound.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Result register(RegisterDTO dto) {
        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(dto.getUsername());
        if (existUser != null) {
            return Result.fail(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success();
    }

    @Override
    public Result login(LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            return Result.fail(ResultCode.PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            return Result.fail(ResultCode.ACCOUNT_DISABLED);
        }
        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        // Token存入Redis（24小时=1440分钟）
        redisUtil.set(Constants.TOKEN_PREFIX + user.getId(), token, 24 * 60);

        // 构建返回数据
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", vo);
        return Result.success(result);
    }

    @Override
    public Result<UserVO> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }

    @Override
    public Result logout(Long userId) {
        // 清除Redis中的Token，实现主动注销
        redisUtil.delete(Constants.TOKEN_PREFIX + userId);
        return Result.success();
    }

    @Override
    public Result<List<UserVO>> getUserList(String keyword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like("username", keyword)
                    .or().like("real_name", keyword)
                    .or().like("phone", keyword)
                    .or().like("email", keyword);
        }
        wrapper.orderByDesc("create_time");
        List<User> users = userMapper.selectList(wrapper);
        List<UserVO> voList = new ArrayList<>();
        for (User user : users) {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            voList.add(vo);
        }
        return Result.success(voList);
    }

    @Override
    public Result updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (user.getRole() == Constants.ROLE_ADMIN) {
            return Result.fail(ResultCode.FORBIDDEN.getCode(), "不能禁用管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        // 如果禁用用户，同时清除其登录Token
        if (status == 0) {
            redisUtil.delete(Constants.TOKEN_PREFIX + userId);
        }
        return Result.success();
    }

    @Override
    public Result updateProfile(Long userId, RegisterDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userMapper.updateById(user);
        return Result.success();
    }

    @Override
    public Result updateAvatar(Long userId, String avatar) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        user.setAvatar(avatar);
        userMapper.updateById(user);
        return Result.success();
    }
}
