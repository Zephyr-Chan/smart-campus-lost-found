package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.dto.LostItemDTO;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.service.LostItemService;
import com.campus.lostfound.vo.LostItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 丢失物品服务实现类
 */
@Service
public class LostItemServiceImpl implements LostItemService {

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private ClaimMapper claimMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 清除指定丢失物品的匹配缓存
     * 兼容多模态/TF-IDF 两种带匹配类型后缀的缓存键（match:lost:{id}:multimodal / :tfidf）
     */
    private void clearMatchCache(Long lostItemId) {
        try {
            java.util.Set<String> keys = redisUtil.getKeys(Constants.MATCH_PREFIX + lostItemId + ":*");
            if (keys != null) {
                for (String k : keys) {
                    redisUtil.delete(k);
                }
            }
        } catch (Exception e) {
            // Redis不可用时忽略
        }
    }

    @Override
    public Result publish(Long userId, LostItemDTO dto) {
        LostItem item = new LostItem();
        item.setUserId(userId);
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setLocation(dto.getLocation());
        item.setEventTime(dto.getEventTime());
        item.setContactInfo(dto.getContactInfo());
        item.setImages(dto.getImages());
        item.setStatus(Constants.STATUS_PENDING);
        lostItemMapper.insert(item);

        // M5修复：发布新丢失物品时清除该物品的匹配缓存
        clearMatchCache(item.getId());

        return Result.success(item);
    }

    @Override
    public Result<IPage<LostItemVO>> getList(int page, int size, String category, String keyword) {
        // M2修复：分页参数校验
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;

        int offset = (page - 1) * size;
        List<LostItemVO> records = lostItemMapper.selectLostItemList(offset, size, category, keyword);

        // 查询总数
        QueryWrapper<LostItem> countWrapper = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            countWrapper.eq("category", category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            countWrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        Long total = lostItemMapper.selectCount(countWrapper);

        Page<LostItemVO> pageResult = new Page<>(page, size);
        pageResult.setRecords(records);
        pageResult.setTotal(total);
        return Result.success(pageResult);
    }

    @Override
    public Result<LostItemVO> getById(Long id) {
        LostItemVO vo = lostItemMapper.selectLostItemById(id);
        if (vo == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.success(vo);
    }

    @Override
    public Result update(Long userId, Long id, LostItemDTO dto) {
        LostItem item = lostItemMapper.selectById(id);
        if (item == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ITEM_NOT_BELONG);
        }
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setLocation(dto.getLocation());
        item.setEventTime(dto.getEventTime());
        item.setContactInfo(dto.getContactInfo());
        item.setImages(dto.getImages());
        lostItemMapper.updateById(item);

        // 更新后清除匹配缓存
        clearMatchCache(id);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long userId, Long id) {
        LostItem item = lostItemMapper.selectById(id);
        if (item == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        if (!item.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ITEM_NOT_BELONG);
        }
        // 已匹配或已关闭的物品不允许删除
        if (item.getStatus() == Constants.STATUS_MATCHED || item.getStatus() == Constants.STATUS_CLOSED) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "物品已匹配或已关闭，无法删除");
        }
        // M3修复：删除物品时同步关闭关联认领记录
        QueryWrapper<Claim> claimWrapper = new QueryWrapper<>();
        claimWrapper.eq("lost_item_id", id);
        List<Claim> claims = claimMapper.selectList(claimWrapper);
        for (Claim claim : claims) {
            if (claim.getStatus() == Constants.CLAIM_PENDING) {
                claim.setStatus(Constants.CLAIM_REJECTED);
                claimMapper.updateById(claim);
            }
        }
        lostItemMapper.deleteById(id);

        // 清除匹配缓存
        clearMatchCache(id);

        return Result.success();
    }
}
