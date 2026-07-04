package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.client.AiMatchingClient;
import com.campus.lostfound.entity.ItemVector;
import com.campus.lostfound.mapper.ItemVectorMapper;
import com.campus.lostfound.service.VectorSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 物品向量同步服务实现
 * 调用 AI 服务建立向量索引，并以 upsert 方式维护 item_vector 表的状态记录。
 * 所有方法均做异常兜底，失败时仅记录告警不抛出，避免阻断物品发布等主流程。
 */
@Service
public class VectorSyncServiceImpl implements VectorSyncService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSyncServiceImpl.class);

    /** 文本向量默认维度（与 AI 服务侧嵌入模型保持一致） */
    private static final int DEFAULT_TEXT_DIM = 512;

    /** 图像向量默认维度（与 AI 服务侧嵌入模型保持一致） */
    private static final int DEFAULT_IMAGE_DIM = 512;

    @Autowired
    private AiMatchingClient aiMatchingClient;

    @Autowired
    private ItemVectorMapper itemVectorMapper;

    @Override
    public void syncItemVector(Long itemId, String itemType, String text, String imagePath) {
        try {
            boolean hasText = text != null && !text.trim().isEmpty();
            boolean hasImage = imagePath != null && !imagePath.trim().isEmpty();

            // 1. 调用 AI 服务建立向量索引
            boolean indexed = aiMatchingClient.indexItem(itemId, itemType, text, imagePath);
            if (!indexed) {
                logger.warn("AI服务索引失败，仅更新本地向量状态, itemId={}, itemType={}", itemId, itemType);
            }

            // 2. upsert item_vector 表（先查再插入/更新）
            QueryWrapper<ItemVector> wrapper = new QueryWrapper<>();
            wrapper.eq("item_id", itemId).eq("item_type", itemType);
            ItemVector existing = itemVectorMapper.selectOne(wrapper);

            if (existing == null) {
                ItemVector iv = new ItemVector();
                iv.setItemId(itemId);
                iv.setItemType(itemType);
                iv.setHasTextVector(hasText ? 1 : 0);
                iv.setHasImageVector(hasImage ? 1 : 0);
                iv.setTextDim(hasText ? DEFAULT_TEXT_DIM : 0);
                iv.setImageDim(hasImage ? DEFAULT_IMAGE_DIM : 0);
                iv.setIndexedAt(LocalDateTime.now());
                itemVectorMapper.insert(iv);
                logger.info("物品向量记录已创建, itemId={}, itemType={}, indexed={}", itemId, itemType, indexed);
            } else {
                existing.setHasTextVector(hasText ? 1 : 0);
                existing.setHasImageVector(hasImage ? 1 : 0);
                existing.setTextDim(hasText ? DEFAULT_TEXT_DIM : 0);
                existing.setImageDim(hasImage ? DEFAULT_IMAGE_DIM : 0);
                existing.setIndexedAt(LocalDateTime.now());
                itemVectorMapper.updateById(existing);
                logger.info("物品向量记录已更新, itemId={}, itemType={}, indexed={}", itemId, itemType, indexed);
            }
        } catch (Exception e) {
            logger.warn("同步物品向量失败, itemId={}, itemType={}: {}", itemId, itemType, e.getMessage());
        }
    }

    @Override
    public void removeItemVector(Long itemId, String itemType) {
        try {
            QueryWrapper<ItemVector> wrapper = new QueryWrapper<>();
            wrapper.eq("item_id", itemId).eq("item_type", itemType);
            int rows = itemVectorMapper.delete(wrapper);
            logger.info("物品向量记录已删除, itemId={}, itemType={}, affected={}", itemId, itemType, rows);
        } catch (Exception e) {
            logger.warn("删除物品向量失败, itemId={}, itemType={}: {}", itemId, itemType, e.getMessage());
        }
    }
}
