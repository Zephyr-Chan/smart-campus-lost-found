package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.exception.BizException;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.common.result.ResultCode;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.dto.FoundItemDTO;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.service.CreditService;
import com.campus.lostfound.service.FoundItemService;
import com.campus.lostfound.vo.FoundItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 拾到物品服务实现类
 */
@Service
public class FoundItemServiceImpl implements FoundItemService {

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private ClaimMapper claimMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CreditService creditService;

    @Override
    public Result publish(Long userId, FoundItemDTO dto) {
        FoundItem item = new FoundItem();
        item.setUserId(userId);
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setLocation(dto.getLocation());
        item.setEventTime(dto.getEventTime());
        item.setContactInfo(dto.getContactInfo());
        item.setImages(dto.getImages());
        item.setStatus(Constants.STATUS_PENDING);
        foundItemMapper.insert(item);

        // M5修复：发布新拾到物品时清除所有匹配缓存
        clearMatchCache();

        // 发布拾到物品奖励信用积分
        try {
            creditService.addCredit(userId, creditService.getPublishFoundScore(),
                    "发布拾到物品", item.getId());
        } catch (Exception e) {
            // 积分奖励失败不影响主流程
        }

        return Result.success(item);
    }

    @Override
    public Result<IPage<FoundItemVO>> getList(int page, int size, String category, String keyword) {
        // M2修复：分页参数校验
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 10;

        int offset = (page - 1) * size;
        List<FoundItemVO> records = foundItemMapper.selectFoundItemList(offset, size, category, keyword);

        // 查询总数
        QueryWrapper<FoundItem> countWrapper = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            countWrapper.eq("category", category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            countWrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        Long total = foundItemMapper.selectCount(countWrapper);

        Page<FoundItemVO> pageResult = new Page<>(page, size);
        pageResult.setRecords(records);
        pageResult.setTotal(total);
        return Result.success(pageResult);
    }

    @Override
    public Result<FoundItemVO> getById(Long id) {
        FoundItemVO vo = foundItemMapper.selectFoundItemById(id);
        if (vo == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.success(vo);
    }

    @Override
    public Result update(Long userId, Long id, FoundItemDTO dto) {
        FoundItem item = foundItemMapper.selectById(id);
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
        foundItemMapper.updateById(item);

        // M5修复：更新拾到物品时清除所有匹配缓存
        clearMatchCache();

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long userId, Long id) {
        FoundItem item = foundItemMapper.selectById(id);
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
        claimWrapper.eq("found_item_id", id);
        List<Claim> claims = claimMapper.selectList(claimWrapper);
        for (Claim claim : claims) {
            if (claim.getStatus() == Constants.CLAIM_PENDING) {
                claim.setStatus(Constants.CLAIM_REJECTED);
                claimMapper.updateById(claim);
            }
        }
        foundItemMapper.deleteById(id);
        clearMatchCache();
        return Result.success();
    }

    /**
     * 清除所有匹配缓存
     */
    private void clearMatchCache() {
        try {
            java.util.Set<String> keys = redisUtil.getKeys(Constants.MATCH_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    redisUtil.delete(key);
                }
            }
        } catch (Exception e) {
            // Redis不可用时忽略
        }
    }
}
