package com.jimuqu.generator.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.generator.domain.GenTemplate;
import com.jimuqu.generator.domain.bo.GenTemplateBo;
import com.jimuqu.generator.domain.vo.GenTemplateVo;
import com.jimuqu.generator.mapper.GenTemplateMapper;
import com.jimuqu.generator.service.IGenTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 代码生成模板Service业务层处理
 *
 * @author chengliang4810
 * @date 2025-01-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenTemplateServiceImpl implements IGenTemplateService {

    private final GenTemplateMapper baseMapper;

    /**
     * 查询代码生成模板
     */
    @Override
    public GenTemplateVo queryById(Long id) {
        return baseMapper.getVoById(id);
    }

    /**
     * 查询代码生成模板列表
     */
    @Override
    public Page<GenTemplateVo> queryPageList(GenTemplateBo bo, PageQuery pageQuery) {
        return buildQueryChain(bo)
                .returnType(GenTemplateVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询代码生成模板列表
     */
    @Override
    public List<GenTemplateVo> queryList(GenTemplateBo bo) {
        QueryChain<GenTemplate> queryChain = buildQueryChain(bo);
        return queryChain.returnType(GenTemplateVo.class).list();
    }

    /**
     * 构建查询条件
     * @param bo 查询对象
     * @return 查询条件对象
     */
    private QueryChain<GenTemplate> buildQueryChain(GenTemplateBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryChain.of(baseMapper)
                .forSearch(true)
                .like(GenTemplate::getName, bo.getName())
                .eq(GenTemplate::getCategory, bo.getCategory())
                .eq(GenTemplate::getEnable, bo.getEnable());
    }

    /**
     * 新增代码生成模板
     */
    @Override
    public Boolean insertByBo(GenTemplateBo bo) {
        GenTemplate add = MapstructUtil.convert(bo, GenTemplate.class);
        boolean flag = baseMapper.save(add, true) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改代码生成模板
     */
    @Override
    public Boolean updateByBo(GenTemplateBo bo) {
        GenTemplate update = MapstructUtil.convert(bo, GenTemplate.class);
        return baseMapper.update(update) > 0;
    }

    /**
     * 批量删除代码生成模板
     */
    @Override
    public Integer deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids);
    }
}
