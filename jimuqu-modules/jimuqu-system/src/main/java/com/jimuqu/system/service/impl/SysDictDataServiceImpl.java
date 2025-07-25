package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
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
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.text.StrUtil;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;


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

    private final SysDictDataMapper sysDictDataMapper;

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
        return buildQueryChain(query)
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
            return ListUtil.zero();
        }
        return QueryChain.of(sysDictDataMapper)
                .returnType(SysDictDataVo.class)
                .eq(SysDictData::getDictTypeKey, dictTypeKey).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysDictData> buildQueryChain(SysDictDataQuery query) {
        return QueryChain.of(sysDictDataMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增字典数据
     */
    @Override
    public Boolean insertByBo(SysDictDataBo bo) {
        SysDictData sysDictData = MapstructUtil.convert(bo, SysDictData.class);
        boolean flag = sysDictDataMapper.save(sysDictData) > 0;
        bo.setId(sysDictData.getId());
        return flag;
    }

    /**
     * 修改字典数据
     */
    @Override
    public Boolean updateByBo(SysDictDataBo bo) {
        SysDictData sysDictData = MapstructUtil.convert(bo, SysDictData.class);
        return sysDictDataMapper.update(sysDictData) > 0;
    }

    /**
     * 批量删除字典数据
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysDictDataMapper.deleteByIds(ids);
    }
}
