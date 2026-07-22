package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jimuqu.common.cache.VersionedCacheNamespace;
import com.jimuqu.common.core.constant.CacheConstants;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysConfig;
import com.jimuqu.system.domain.bo.SysConfigBo;
import com.jimuqu.system.domain.vo.SysConfigVo;
import com.jimuqu.system.domain.query.SysConfigQuery;
import com.jimuqu.system.mapper.SysConfigMapper;
import com.jimuqu.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.cache.CacheService;

import java.util.Collection;
import java.util.List;


/**
 * 参数配置Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private static final int CACHE_TTL_SECONDS = 0;

    private final SysConfigMapper sysConfigMapper;
    private final CacheService cacheService;

    /**
     * 查询参数配置
     */
    @Override
    public SysConfigVo queryById(Long id) {
        return sysConfigMapper.getVoById(id);
    }

    /**
     * 查询参数配置分页列表
     */
    @Override
    public Page<SysConfigVo> queryPageList(SysConfigQuery query, PageQuery pageQuery) {
        return pageQuery.applyOrder(buildQueryChain(query))
                .returnType(SysConfigVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询参数配置列表
     */
    @Override
    public List<SysConfigVo> queryList(SysConfigQuery query) {
        QueryChain<SysConfig> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysConfigVo.class).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysConfig> buildQueryChain(SysConfigQuery query) {
        return QueryChain.of(sysConfigMapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysConfig::getId);
    }

    /**
     * 新增参数配置
     */
    @Override
    public Boolean insertByBo(SysConfigBo bo) {
        SysConfig sysConfig = MapstructUtil.convert(bo, SysConfig.class);
        boolean flag = sysConfigMapper.save(sysConfig) > 0;
        bo.setId(sysConfig.getId());
        if (flag) {
            storeConfigCache(sysConfig.getConfigKey(), sysConfig.getConfigValue());
        }
        return flag;
    }

    /**
     * 修改参数配置
     */
    @Override
    public Boolean updateByBo(SysConfigBo bo) {
        SysConfig old = bo.getId() == null ? null : sysConfigMapper.getById(bo.getId());
        SysConfig sysConfig = MapstructUtil.convert(bo, SysConfig.class);
        boolean updated = sysConfigMapper.update(sysConfig) > 0;
        if (updated) {
            if (old != null && !ObjectUtil.equals(old.getConfigKey(), sysConfig.getConfigKey())) {
                evictConfigCache(old.getConfigKey());
            }
            storeConfigCache(sysConfig.getConfigKey(), sysConfig.getConfigValue());
        }
        return updated;
    }

    @Override
    public Boolean updateByKey(SysConfigBo bo) {
        SysConfig current = QueryChain.of(sysConfigMapper)
                .eq(SysConfig::getConfigKey, bo.getConfigKey())
                .get();
        if (current == null) {
            return false;
        }
        bo.setId(current.getId());
        return updateByBo(bo);
    }

    @Override
    public String selectConfigByKey(String configKey) {
        if (StrUtil.isBlank(configKey)) {
            return "";
        }
        return configCache().getOrStore(configKey, String.class, CACHE_TTL_SECONDS, () -> {
            SysConfig config = QueryChain.of(sysConfigMapper)
                    .eq(SysConfig::getConfigKey, configKey)
                    .select(SysConfig::getConfigValue)
                    .get();
            return config == null || config.getConfigValue() == null ? "" : config.getConfigValue();
        });
    }

    @Override
    public boolean selectRegisterEnabled() {
        return Boolean.parseBoolean(selectConfigByKey("sys.account.registerUser"));
    }

    @Override
    public boolean checkConfigKeyUnique(SysConfigBo bo) {
        return !QueryChain.of(sysConfigMapper)
                .eq(SysConfig::getConfigKey, bo.getConfigKey())
                .ne(ObjectUtil.isNotNull(bo.getId()), SysConfig::getId, bo.getId())
                .exists();
    }

    /**
     * 批量删除参数配置
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        List<Long> requested = ids.stream().distinct().toList();
        List<SysConfig> configs = QueryChain.of(sysConfigMapper).in(SysConfig::getId, requested).list();
        if (configs.size() != requested.size()) {
            throw new ServiceException("参数配置不存在");
        }
        for (SysConfig config : configs) {
            if ("Y".equals(config.getConfigType())) {
                throw new ServiceException("内置参数【" + config.getConfigKey() + "】不能删除");
            }
        }
        int rows = sysConfigMapper.deleteByIds(requested);
        if (rows > 0) {
            configs.forEach(config -> evictConfigCache(config.getConfigKey()));
        }
        return rows;
    }

    @Override
    public void resetConfigCache() {
        configCache().refresh();
    }

    private void storeConfigCache(String configKey, String configValue) {
        if (StrUtil.isNotBlank(configKey)) {
            configCache().store(configKey, configValue == null ? "" : configValue, CACHE_TTL_SECONDS);
        }
    }

    private void evictConfigCache(String configKey) {
        if (StrUtil.isNotBlank(configKey)) {
            configCache().remove(configKey);
        }
    }

    private VersionedCacheNamespace configCache() {
        return new VersionedCacheNamespace(cacheService, CacheConstants.SYS_CONFIG_KEY);
    }
}
