package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.entity.SysConfig;
import com.campushelp.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务（带本地缓存）
 */
@Service
@RequiredArgsConstructor
public class SysConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {

    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    /**
     * 获取字符串配置
     */
    public String getConfigValue(String key) {
        return localCache.computeIfAbsent(key, k -> {
            SysConfig config = this.getOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, k));
            return config != null ? config.getConfigValue() : "";
        });
    }

    /**
     * 获取 BigDecimal 配置
     */
    public BigDecimal getConfigDecimal(String key, BigDecimal defaultValue) {
        String value = getConfigValue(key);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取 int 配置
     */
    public int getConfigInt(String key, int defaultValue) {
        String value = getConfigValue(key);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        localCache.clear();
    }
}
