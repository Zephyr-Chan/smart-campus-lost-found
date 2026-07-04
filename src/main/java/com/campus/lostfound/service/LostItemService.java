package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.dto.LostItemDTO;
import com.campus.lostfound.vo.LostItemVO;

/**
 * 丢失物品服务接口
 */
public interface LostItemService {

    /**
     * 发布丢失物品
     */
    Result publish(Long userId, LostItemDTO dto);

    /**
     * 分页查询丢失物品列表
     */
    Result<IPage<LostItemVO>> getList(int page, int size, String category, String keyword);

    /**
     * 查询丢失物品详情
     */
    Result<LostItemVO> getById(Long id);

    /**
     * 编辑丢失物品
     */
    Result update(Long userId, Long id, LostItemDTO dto);

    /**
     * 删除丢失物品
     */
    Result delete(Long userId, Long id);
}
