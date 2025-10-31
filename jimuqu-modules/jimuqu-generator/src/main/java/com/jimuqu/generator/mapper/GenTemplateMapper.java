package com.jimuqu.generator.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.generator.domain.GenTemplate;
import com.jimuqu.generator.domain.vo.GenTemplateVo;

import java.util.List;

/**
 * 代码生成模板Mapper接口
 *
 * @author chengliang4810
 * @date 2025-01-05
 */
public interface GenTemplateMapper extends BaseMapperPlus<GenTemplate, GenTemplateVo> {

    /**
     * 查询启用的模板
     *
     * @return 模板列表
     */
    default List<GenTemplateVo> selectEnableList() {
        return QueryChain.of(this)
                .eq(GenTemplate::getEnable, true)
                .returnType(GenTemplateVo.class)
                .list();
    }

}
