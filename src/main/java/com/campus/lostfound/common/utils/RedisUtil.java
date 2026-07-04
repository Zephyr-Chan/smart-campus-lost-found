package com.campus.lostfound.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis操作工具类
 * 所有方法均做了异常兜底处理，Redis不可用时不会影响主流程
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeoutMinutes) {
        try {
            String strValue = value == null ? "" : value.toString();
            stringRedisTemplate.opsForValue().set(key, strValue, timeoutMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis写入失败, key={}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis读取失败, key={}, 错误: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        try {
            return stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis删除失败, key={}, 错误: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 根据模式获取所有匹配的key（使用SCAN，避免KEYS命令阻塞Redis）
     */
    public java.util.Set<String> getKeys(String pattern) {
        try {
            java.util.Set<String> keys = new java.util.HashSet<>();
            stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<java.util.Set<String>>) connection -> {
                try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match(pattern)
                                .count(100)
                                .build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                } catch (Exception e) {
                    log.warn("Redis SCAN失败, pattern={}, 错误: {}", pattern, e.getMessage());
                }
                return keys;
            });
            return keys;
        } catch (Exception e) {
            log.warn("Redis查询keys失败, pattern={}, 错误: {}", pattern, e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 判断key是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return stringRedisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Redis查询失败, key={}, 错误: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public void expire(String key, long timeoutMinutes) {
        try {
            stringRedisTemplate.expire(key, timeoutMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis设置过期失败, key={}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 自增
     */
    public Long incr(String key) {
        try {
            return stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Redis自增失败, key={}, 错误: {}", key, e.getMessage());
            return 0L;
        }
    }

    /**
     * 检查Redis是否可用
     *
     * @return true=可用, false=不可用
     */
    public boolean isAvailable() {
        try {
            return stringRedisTemplate.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
