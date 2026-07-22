package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.auth.service.MiniProgramIdentityAdapter;
import com.jimuqu.common.cache.VersionedCacheNamespace;
import com.jimuqu.common.core.constant.CacheConstants;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysDictData;
import com.jimuqu.system.domain.bo.SysDictDataBo;
import com.jimuqu.system.domain.query.SysDictDataQuery;
import com.jimuqu.system.domain.vo.SysDictDataVo;
import com.jimuqu.system.mapper.SysDictDataMapper;
import com.jimuqu.system.service.SysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.cache.CacheService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * 字典数据Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysDictDataServiceImpl implements SysDictDataService {

    private static final int CACHE_TTL_SECONDS = 0;

    private final SysDictDataMapper sysDictDataMapper;
    private final CacheService cacheService;
    private final MiniProgramIdentityAdapter miniProgramIdentityAdapter;

    /**
     * 查询字典数据
     */
    @Override
    public SysDictDataVo queryById(Long id) {
        return sysDictDataMapper.getVoById(id);
    }

    /**
     * 查询字典数据分页列表
     */
    @Override
    public Page<SysDictDataVo> queryPageList(SysDictDataQuery query, PageQuery pageQuery) {
        return pageQuery.applyOrder(buildQueryChain(query))
                .returnType(SysDictDataVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询字典数据列表
     */
    @Override
    public List<SysDictDataVo> queryList(SysDictDataQuery query) {
        QueryChain<SysDictData> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysDictDataVo.class).list();
    }

    /**
     * 按类型键查询列表
     *
     * @param dictTypeKey 字典类型键
     * @return {@link List }<{@link SysDictDataVo }>
     */
    @Override
    public List<SysDictDataVo> queryListByTypeKey(String dictTypeKey) {
        if (StrUtil.isBlank(dictTypeKey)) {
            return java.util.Collections.emptyList();
        }
        SysDictDataVo[] values = dictCache().getOrStore(
                dictTypeKey, SysDictDataVo[].class, CACHE_TTL_SECONDS,
                () -> QueryChain.of(sysDictDataMapper)
                        .returnType(SysDictDataVo.class)
                        .eq(SysDictData::getDictTypeKey, dictTypeKey)
                        .orderBy(SysDictData::getDictSort, SysDictData::getId)
                        .list()
                        .toArray(SysDictDataVo[]::new));
        return hideUnavailableMiniProgramOptions(
                dictTypeKey, List.of(values), miniProgramIdentityAdapter.isAvailable());
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysDictData> buildQueryChain(SysDictDataQuery query) {
        return QueryChain.of(sysDictDataMapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysDictData::getDictSort, SysDictData::getId);
    }

    /**
     * 新增字典数据
     */
    @Override
    public Boolean insertByBo(SysDictDataBo bo) {
        SysDictData sysDictData = MapstructUtil.convert(bo, SysDictData.class);
        boolean flag = sysDictDataMapper.save(sysDictData) > 0;
        bo.setId(sysDictData.getId());
        if (flag) {
            evictDictCache(sysDictData.getDictTypeKey());
        }
        return flag;
    }

    /**
     * 修改字典数据
     */
    @Override
    public Boolean updateByBo(SysDictDataBo bo) {
        SysDictData old = bo.getId() == null ? null : sysDictDataMapper.getById(bo.getId());
        SysDictData sysDictData = MapstructUtil.convert(bo, SysDictData.class);
        boolean updated = sysDictDataMapper.update(sysDictData) > 0;
        if (updated) {
            if (old != null) {
                evictDictCache(old.getDictTypeKey());
            }
            evictDictCache(sysDictData.getDictTypeKey());
        }
        return updated;
    }

    @Override
    public boolean checkDictDataUnique(SysDictDataBo bo) {
        return !QueryChain.of(sysDictDataMapper)
                .eq(SysDictData::getDictTypeKey, bo.getDictTypeKey())
                .eq(SysDictData::getDictValue, bo.getDictValue())
                .ne(bo.getId() != null, SysDictData::getId, bo.getId())
                .exists();
    }

    /**
     * 批量删除字典数据
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        List<Long> requested = ids.stream().distinct().toList();
        List<SysDictData> dictData = QueryChain.of(sysDictDataMapper)
                .in(SysDictData::getId, requested)
                .list();
        if (dictData.size() != requested.size()) {
            throw new ServiceException("字典数据不存在");
        }
        int rows = sysDictDataMapper.deleteByIds(requested);
        if (rows > 0) {
            dictData.stream().map(SysDictData::getDictTypeKey)
                    .filter(Objects::nonNull).distinct().forEach(this::evictDictCache);
        }
        return rows;
    }

    private void evictDictCache(String dictTypeKey) {
        if (StrUtil.isNotBlank(dictTypeKey)) {
            dictCache().remove(dictTypeKey);
        }
    }

    private VersionedCacheNamespace dictCache() {
        return new VersionedCacheNamespace(cacheService, CacheConstants.SYS_DICT_KEY);
    }

    static List<SysDictDataVo> hideUnavailableMiniProgramOptions(
            String dictTypeKey, List<SysDictDataVo> values, boolean miniProgramAvailable) {
        if (miniProgramAvailable
                || !Set.of("sys_grant_type", "sys_device_type").contains(dictTypeKey)) {
            return values;
        }
        return values.stream()
                .filter(value -> !"xcx".equals(value.getDictValue()))
                .toList();
    }
}
