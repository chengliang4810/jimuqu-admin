package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.Where;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysApiKey;
import com.jimuqu.system.domain.vo.SysApiKeyVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * API密钥数据层
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Mapper
public interface SysApiKeyMapper extends BaseMapperPlus<SysApiKey, SysApiKeyVo> {

    /**
     * 根据条件分页查询API密钥列表
     *
     * @param pageQuery 分页参数
     * @param queryWrapper 查询条件
     * @return API密钥分页列表
     */
    default Page<SysApiKeyVo> selectPageApiKeyList(PageQuery pageQuery, Where queryWrapper) {
        return QueryChain.of(this, queryWrapper)
                .where(queryWrapper)
                .returnType(SysApiKeyVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 根据条件查询API密钥列表
     *
     * @param queryWrapper 查询条件
     * @return API密钥列表
     */
    default List<SysApiKeyVo> selectApiKeyList(Where queryWrapper) {
        return QueryChain.of(this, queryWrapper)
                .where(queryWrapper)
                .returnType(SysApiKeyVo.class)
                .list();
    }

    /**
     * 根据API Key值查询API密钥
     *
     * @param apiKey API Key值
     * @return API密钥对象信息
     */
    default SysApiKeyVo selectApiKeyByKey(String apiKey) {
        return QueryChain.of(this)
                .eq(SysApiKey::getApiKey, apiKey)
                .returnType(SysApiKeyVo.class)
                .get();
    }

    /**
     * 根据用户ID查询API密钥列表
     *
     * @param loginId 用户ID
     * @return API密钥列表
     */
    default List<SysApiKeyVo> selectApiKeyListByLoginId(Long loginId) {
        return QueryChain.of(this)
                .eq(SysApiKey::getUserId, loginId)
                .returnType(SysApiKeyVo.class)
                .list();
    }

    /**
     * 根据ID查询API密钥
     *
     * @param id API密钥ID
     * @return API密钥对象信息
     */
    default SysApiKeyVo selectApiKeyById(Long id) {
        return QueryChain.of(this)
                .eq(SysApiKey::getId, id)
                .returnType(SysApiKeyVo.class)
                .get();
    }

    /**
     * 根据命名空间和API Key查询API密钥模型
     *
     * @param namespace 命名空间
     * @param apiKey API Key值
     * @return API密钥对象信息
     */
    default SysApiKey getApiKeyModel(String namespace, String apiKey) {
        return QueryChain.of(this)
                .eq(SysApiKey::getNamespace, namespace)
                .eq(SysApiKey::getApiKey, apiKey)
                .eq(SysApiKey::getIsValid, true)
                .get();
    }
}