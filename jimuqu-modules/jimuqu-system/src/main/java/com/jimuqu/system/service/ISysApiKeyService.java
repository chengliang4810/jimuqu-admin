package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysApiKey;
import com.jimuqu.system.domain.bo.SysApiKeyBo;
import com.jimuqu.system.domain.vo.SysApiKeyVo;
import com.jimuqu.system.domain.query.SysApiKeyQuery;

import java.util.Collection;
import java.util.List;

/**
 * API密钥服务接口
 * @author jimuqu-admin
 * @since 2025-08-18
 */
public interface ISysApiKeyService {

    /**
     * 根据主键查询API密钥
     *
     * @param id API密钥主键
     * @return API密钥视图对象
     */
    SysApiKeyVo queryById(Long id);

    /**
     * 根据API Key值查询API密钥
     *
     * @param apiKey API Key值
     * @return API密钥对象
     */
    SysApiKey queryByKey(String apiKey);

    /**
     * 根据用户ID查询API密钥列表
     *
     * @param userId 用户ID
     * @return API密钥列表
     */
    List<SysApiKeyVo> queryByUserId(Long userId);

    /**
     * 查询API密钥分页列表
     *
     * @param query 查询条件
     * @param pageQuery 分页参数
     * @return API密钥分页列表
     */
    Page<SysApiKeyVo> queryPageList(SysApiKeyQuery query, PageQuery pageQuery);

    /**
     * 查询API密钥列表
     *
     * @param query 查询条件
     * @return API密钥列表
     */
    List<SysApiKeyVo> queryList(SysApiKeyQuery query);

    /**
     * 新增API密钥
     *
     * @param bo API密钥业务对象
     * @return 新增是否成功
     */
    Boolean insertByBo(SysApiKeyBo bo);

    /**
     * 更新API密钥
     *
     * @param bo API密钥业务对象
     * @return 更新是否成功
     */
    Boolean updateByBo(SysApiKeyBo bo);

    /**
     * 修改API密钥状态
     *
     * @param id 主键
     * @param isValid 是否有效
     * @return 修改是否成功
     */
    boolean updateStatus(Long id, Boolean isValid);

    /**
     * 批量删除API密钥
     *
     * @param ids API密钥主键列表
     * @return 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);

    /**
     * 删除API密钥
     *
     * @param id API密钥主键
     * @return 删除是否成功
     */
    boolean deleteById(Long id);

    /**
     * 生成新的API Key
     *
     * @return 生成的API Key
     */
    String generateApiKey();
}