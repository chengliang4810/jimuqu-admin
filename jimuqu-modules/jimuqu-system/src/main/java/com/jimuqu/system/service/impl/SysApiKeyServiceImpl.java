package com.jimuqu.system.service.impl;

import cn.dev33.satoken.apikey.loader.SaApiKeyDataLoader;
import cn.dev33.satoken.apikey.model.ApiKeyModel;
import cn.dev33.satoken.apikey.template.SaApiKeyUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.v7.core.data.id.IdUtil;
import cn.hutool.v7.core.text.StrPool;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysApiKey;
import com.jimuqu.system.domain.bo.SysApiKeyBo;
import com.jimuqu.system.domain.query.SysApiKeyQuery;
import com.jimuqu.system.domain.vo.SysApiKeyVo;
import com.jimuqu.system.mapper.SysApiKeyMapper;
import com.jimuqu.system.service.ISysApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * API密钥服务实现
 *
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysApiKeyServiceImpl implements ISysApiKeyService, SaApiKeyDataLoader {

    private final SysApiKeyMapper sysApiKeyMapper;

    /**
     * 指定框架不再维护 API Key 索引信息，而是由我们手动从数据库维护
     *
     * @return 是否记录索引信息
     */
    @Override
    public Boolean getIsRecordIndex() {
        return false;
    }

    /**
     * 根据 apiKey 从数据库获取 ApiKeyModel 信息 （实现此方法无需为数据做缓存处理，框架内部已包含缓存逻辑）
     */
    @Override
    public ApiKeyModel getApiKeyModelFromDatabase(String namespace, String apiKey) {

        SysApiKey sysApiKey = sysApiKeyMapper.getApiKeyModel(StrUtil.blankToDefault(namespace, "default"), apiKey);
        if (sysApiKey == null) {
            return null;
        }
        
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey(sysApiKey.getApiKey());
        apiKeyModel.setLoginId(sysApiKey.getUserId().toString());
        
        // 设置权限范围
        if (StrUtil.isNotBlank(sysApiKey.getScope())) {
            String[] scopes = sysApiKey.getScope().split(",");
            apiKeyModel.addScope(scopes);
        }
        
        // 设置过期时间
        if (sysApiKey.getExpiresTime() != null) {
            apiKeyModel.setExpiresTime(sysApiKey.getExpiresTime());
        }
        
        // 设置扩展信息
        if (StrUtil.isNotBlank(sysApiKey.getExtraData())) {
            apiKeyModel.setExtraData(sysApiKey.getExtraMap());
        }
        
        return apiKeyModel;
    }

    /**
     * 查询API密钥
     */
    @Override
    public SysApiKeyVo queryById(Long id) {
        SysApiKeyVo sysApiKey = sysApiKeyMapper.getVoById(id);
        // 非超级管理员只能查看自己的API密钥
        if (sysApiKey != null && !LoginHelper.isSuperAdmin() && 
            !sysApiKey.getUserId().equals(LoginHelper.getUserId())) {
            return null;
        }
        return sysApiKey;
    }

    /**
     * 根据API Key值查询API密钥
     */
    @Override
    public SysApiKey queryByKey(String apiKey) {
        SysApiKey sysApiKey = sysApiKeyMapper.get(where -> where.eq(SysApiKey::getApiKey, apiKey));
        // 非超级管理员只能查看自己的API密钥
        if (sysApiKey != null && !LoginHelper.isSuperAdmin() && 
            !sysApiKey.getUserId().equals(LoginHelper.getUserId())) {
            return null;
        }
        return sysApiKey;
    }

    /**
     * 根据用户ID查询API密钥列表
     */
    @Override
    public List<SysApiKeyVo> queryByUserId(Long userId) {
        // 非超级管理员只能查询自己的API密钥
        if (!LoginHelper.isSuperAdmin() && !userId.equals(LoginHelper.getUserId())) {
            return List.of();
        }
        return sysApiKeyMapper.selectApiKeyListByLoginId(userId);
    }

    /**
     * 查询API密钥分页列表
     */
    @Override
    public Page<SysApiKeyVo> queryPageList(SysApiKeyQuery query, PageQuery pageQuery) {
        QueryChain<SysApiKey> queryChain = buildQueryChain(query);
        // 非超级管理员只能查看自己的API密钥
        if (!LoginHelper.isSuperAdmin()) {
            queryChain.eq(SysApiKey::getUserId, LoginHelper.getUserId());
        }
        return queryChain.returnType(SysApiKeyVo.class).paging(pageQuery.build());
    }

    /**
     * 查询API密钥列表
     */
    @Override
    public List<SysApiKeyVo> queryList(SysApiKeyQuery query) {
        QueryChain<SysApiKey> queryChain = buildQueryChain(query);
        // 非超级管理员只能查看自己的API密钥
        if (!LoginHelper.isSuperAdmin()) {
            queryChain.eq(SysApiKey::getUserId, LoginHelper.getUserId());
        }
        return queryChain.returnType(SysApiKeyVo.class).list();
    }

    /**
     * 构建查询条件
     */
    private QueryChain<SysApiKey> buildQueryChain(SysApiKeyQuery query) {
        return QueryChain.of(sysApiKeyMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增API密钥
     */
    @Override
    public Boolean insertByBo(SysApiKeyBo bo) {
        // 非超级管理员只能创建自己的API密钥
        if (!LoginHelper.isSuperAdmin()) {
            bo.setUserId(LoginHelper.getUserId());
        }
        // 生成 API Key
        String apiKey = this.generateApiKey();
        bo.setApiKey(apiKey);

        if (StrUtil.isNotBlank(bo.getNamespace())){
            bo.setNamespace("default");
        }

        // 入库
        SysApiKey sysApiKey = MapstructUtil.convert(bo, SysApiKey.class);
        boolean flag = sysApiKeyMapper.save(sysApiKey) > 0;
        bo.setId(sysApiKey.getId());

        // 构建SaToken API Key 模型
        ApiKeyModel akModel = new ApiKeyModel();
        akModel.setLoginId(sysApiKey.getUserId().toString());  // 设置绑定的用户 id
        akModel.setApiKey(apiKey);
        akModel.setTitle(sysApiKey.getName());	  // 设置名称
        akModel.setIntro(sysApiKey.getRemark());   // 设置描述

        // 设置权限范围
        if (StrUtil.isNotBlank(sysApiKey.getScope())) {
            String[] scopes = sysApiKey.getScope().split(StrPool.COMMA);
            akModel.addScope(scopes);
        }

        // 设置过期时间
        akModel.setExpiresTime(sysApiKey.getExpiresTime());

        // 设置扩展信息
        if (StrUtil.isNotBlank(sysApiKey.getExtraData())) {
            akModel.setExtraData(sysApiKey.getExtraMap());
        }

        // 保存
        SaApiKeyUtil.saveApiKey(akModel);

        return flag;
    }

    /**
     * 更新API密钥
     */
    @Override
    public Boolean updateByBo(SysApiKeyBo bo) {
        // 非超级管理员只能更新自己的API密钥
        if (!LoginHelper.isSuperAdmin()) {
            SysApiKey existingApiKey = sysApiKeyMapper.getById(bo.getId());
            if (existingApiKey == null || !existingApiKey.getUserId().equals(LoginHelper.getUserId())) {
                return false;
            }
            // 确保不能修改userId
            bo.setUserId(LoginHelper.getUserId());
        }
        SysApiKey sysApiKey = MapstructUtil.convert(bo, SysApiKey.class);
        return sysApiKeyMapper.update(sysApiKey) > 0;
    }

    /**
     * 修改API密钥状态
     */
    @Override
    public boolean updateStatus(Long id, Boolean isValid) {
        // 非超级管理员只能修改自己的API密钥状态
        if (!LoginHelper.isSuperAdmin()) {
            SysApiKey existingApiKey = sysApiKeyMapper.getById(id);
            if (existingApiKey == null || !existingApiKey.getUserId().equals(LoginHelper.getUserId())) {
                return false;
            }
        }
        return sysApiKeyMapper.update(new SysApiKey().setId(id).setIsValid(isValid)) > 0;
    }

    /**
     * 批量删除API密钥
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        List<String> apiKeyList = new ArrayList<>();
        // 非超级管理员只能删除自己的API密钥
        if (!LoginHelper.isSuperAdmin()) {
            Long currentUserId = LoginHelper.getUserId();
            List<SysApiKey> apiKeys = sysApiKeyMapper.listByIds(ids);
            for (SysApiKey apiKey : apiKeys) {
                if (!apiKey.getUserId().equals(currentUserId)) {
                    return 0;
                }
                apiKeyList.add(apiKey.getApiKey());
            }
        }
        // 删除API密钥
        apiKeyList.forEach(SaApiKeyUtil::deleteApiKey);
        return sysApiKeyMapper.deleteByIds(ids);
    }

    /**
     * 删除API密钥
     */
    @Override
    public boolean deleteById(Long id) {
        String apiKey = null;
        // 非超级管理员只能删除自己的API密钥
        if (!LoginHelper.isSuperAdmin()) {
            SysApiKey existingApiKey = sysApiKeyMapper.getById(id);
            if (existingApiKey == null || !existingApiKey.getUserId().equals(LoginHelper.getUserId())) {
                return false;
            }
            apiKey = existingApiKey.getApiKey();
        }
        SaApiKeyUtil.deleteApiKey(apiKey);
        return sysApiKeyMapper.deleteById(id) > 0;
    }

    /**
     * 生成新的API Key
     */
    @Override
    public String generateApiKey() {
        return "AK-" + IdUtil.fastSimpleUUID();
    }
}