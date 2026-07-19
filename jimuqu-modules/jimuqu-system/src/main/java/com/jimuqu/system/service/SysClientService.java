package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.bo.SysClientBo;
import com.jimuqu.system.domain.vo.SysClientVo;
import com.jimuqu.system.domain.query.SysClientQuery;

import java.util.Collection;
import java.util.List;

/**
 * 授权管理对象 sys_clientService接口
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
public interface SysClientService {

    /**
     * 根据主键查询授权管理对象 sys_client
     *
     * @param id 授权管理对象 sys_client主键
     * @return {@link SysClientVo } 授权管理对象 sys_client视图对象
     */
    SysClientVo queryById(Long id);

    /**
     * 查询客户端信息基于客户端id
     *
     * @param clientId 客户端ID
     * @return {@link SysClient }
     */
    SysClient queryByClientId(String clientId);

    /**
     * 查询授权管理对象 sys_client分页列表
     *
     * @param query 查询条件对象
     * @param pageQuery 分页条件
     * @return {@link Page }<{@link SysClientVo }> 授权管理对象 sys_client分页对象
     */
    Page<SysClientVo> queryPageList(SysClientQuery query, PageQuery pageQuery);

    /**
     * 查询授权管理对象 sys_client列表
     *
     * @param query 查询条件对象
     * @return {@link List }<{@link SysClientVo }> 授权管理对象 sys_client列表
     */
    List<SysClientVo> queryList(SysClientQuery query);

    /**
     * 新增授权管理对象 sys_client
     *
     * @param bo 授权管理对象 sys_client业务对象
     * @return {@link Boolean } 新增是否成功
     */
    Boolean insertByBo(SysClientBo bo);

    /**
     * 更新授权管理对象 sys_client
     *
     * @param bo 授权管理对象 sys_client业务对象
     * @return {@link Boolean } 更新是否成功
     */
    Boolean updateByBo(SysClientBo bo);

    /**
     * 修改状态
     *
     * @param id     主键
     * @param status 状态
     * @return boolean
     */
    boolean updateUserStatus(Long id, String status);

    /**
     * 按客户端标识修改状态
     */
    boolean updateClientStatus(String clientId, String status);

    /**
     * 校验客户端 key 唯一性
     */
    boolean checkClientKeyUnique(SysClientBo bo);

    /**
     * 批量删除客户端信息
     *
     * @param ids 授权管理对象 sys_client主键列表
     * @return {@link Integer } 删除成功条数
     */
    Integer deleteByIds(Collection<Long> ids);
}
