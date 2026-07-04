package com.campus.lostfound.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.lostfound.common.constant.Constants;
import com.campus.lostfound.common.utils.RedisUtil;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务 - 超期物品自动归档
 */
@Slf4j
@Component
public class ScheduledTask {

    @Autowired
    private LostItemMapper lostItemMapper;

    @Autowired
    private FoundItemMapper foundItemMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 每日凌晨2点执行，将超期30天未处理的物品自动关闭
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveExpiredItems() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        log.info("开始执行超期物品归档任务, 阈值时间={}", threshold);

        // 归档超期的丢失物品
        LambdaQueryWrapper<LostItem> lostQuery = new LambdaQueryWrapper<>();
        lostQuery.eq(LostItem::getStatus, Constants.STATUS_PENDING)
                .lt(LostItem::getCreateTime, threshold);
        java.util.List<LostItem> expiredLostItems = lostItemMapper.selectList(lostQuery);
        if (!expiredLostItems.isEmpty()) {
            LambdaUpdateWrapper<LostItem> lostUpdate = new LambdaUpdateWrapper<>();
            lostUpdate.eq(LostItem::getStatus, Constants.STATUS_PENDING)
                    .lt(LostItem::getCreateTime, threshold)
                    .set(LostItem::getStatus, Constants.STATUS_CLOSED);
            lostItemMapper.update(null, lostUpdate);
            log.info("已归档超期丢失物品 {} 件", expiredLostItems.size());
            // 清除Redis缓存
            for (LostItem item : expiredLostItems) {
                try {
                    // 兼容带匹配类型后缀的缓存键（match:lost:{id}:multimodal / :tfidf）
                    java.util.Set<String> keys = redisUtil.getKeys(Constants.MATCH_PREFIX + item.getId() + ":*");
                    if (keys != null) {
                        for (String k : keys) {
                            redisUtil.delete(k);
                        }
                    }
                } catch (Exception e) {
                    log.warn("清除Redis缓存失败, itemId={}", item.getId());
                }
            }
        }

        // 归档超期的拾到物品
        LambdaQueryWrapper<FoundItem> foundQuery = new LambdaQueryWrapper<>();
        foundQuery.eq(FoundItem::getStatus, Constants.STATUS_PENDING)
                .lt(FoundItem::getCreateTime, threshold);
        java.util.List<FoundItem> expiredFoundItems = foundItemMapper.selectList(foundQuery);
        if (!expiredFoundItems.isEmpty()) {
            LambdaUpdateWrapper<FoundItem> foundUpdate = new LambdaUpdateWrapper<>();
            foundUpdate.eq(FoundItem::getStatus, Constants.STATUS_PENDING)
                    .lt(FoundItem::getCreateTime, threshold)
                    .set(FoundItem::getStatus, Constants.STATUS_CLOSED);
            foundItemMapper.update(null, foundUpdate);
            log.info("已归档超期拾到物品 {} 件", expiredFoundItems.size());
        }

        log.info("超期物品归档任务执行完毕");
    }
}
