package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.lostfound.common.result.Result;
import com.campus.lostfound.dto.FoundItemDTO;
import com.campus.lostfound.vo.FoundItemVO;

/**
 * 拾到物品服务接口
 */
public interface FoundItemService {

    /**
     * 发布拾到物品
     */
    Result publish(Long userId, FoundItemDTO dto);

    /**
     * 分页查询拾到物品列表
     */
    Result<IPage<FoundItemVO>> getList(int page, int size, String category, String keyword);

    /**
     * 查询拾到物品详情
     */
    Result<FoundItemVO> getById(Long id);

    /**
     * 编辑拾到物品
     */
    Result update(Long userId, Long id, FoundItemDTO dto);

    /**
     * 删除拾到物品
     */
    Result delete(Long userId, Long id);
}
