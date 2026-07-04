package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.vo.LostItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 丢失物品Mapper
 */
@Mapper
public interface LostItemMapper extends BaseMapper<LostItem> {

    /**
     * 分页查询丢失物品列表（关联user表查发布者姓名）
     *
     * @param offset   偏移量
     * @param size     每页数量
     * @param category 分类
     * @param keyword  关键词
     * @return 丢失物品VO列表
     */
    List<LostItemVO> selectLostItemList(@Param("offset") int offset,
                                        @Param("size") int size,
                                        @Param("category") String category,
                                        @Param("keyword") String keyword);

    /**
     * 根据ID查询丢失物品详情（关联user表）
     *
     * @param id 物品ID
     * @return 丢失物品VO
     */
    LostItemVO selectLostItemById(@Param("id") Long id);
}
