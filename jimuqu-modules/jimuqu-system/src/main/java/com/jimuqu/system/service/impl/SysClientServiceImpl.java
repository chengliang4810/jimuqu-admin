package com.jimuqu.system.service.impl;

import cn.hutool.v7.crypto.SecureUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.bo.SysClientBo;
import com.jimuqu.system.domain.query.SysClientQuery;
import com.jimuqu.system.domain.vo.SysClientVo;
import com.jimuqu.system.mapper.SysClientMapper;
import com.jimuqu.system.service.SysClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;


/**
 * 授权管理对象 sys_clientService业务层处理
 *
 * @author chengliang4810
 * @since 2025-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysClientServiceImpl implements SysClientService {

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

    private final SysClientMapper sysClientMapper;

    /**
     * 查询授权管理对象 sys_client
     */
    @Override
    public SysClientVo queryById(Long id) {
        return fillRuleFields(sysClientMapper.getVoById(id));
    }

    /**
     * 查询客户端信息基于客户端id
     *
     * @param clientId 客户端Id
     */
    @Override
    public SysClient queryByClientId(String clientId) {
        return sysClientMapper.get(where -> where.eq(SysClient::getClientId, clientId));
    }

    /**
     * 查询授权管理对象 sys_client分页列表
     */
    @Override
    public Page<SysClientVo> queryPageList(SysClientQuery query, PageQuery pageQuery) {
        Page<SysClient> entityPage = buildQueryChain(query).paging(pageQuery.build());
        List<SysClientVo> rows = MapstructUtil.convert(entityPage.getRows(), SysClientVo.class);
        rows.forEach(this::fillRuleFields);
        return Page.of(rows, entityPage.getTotal());
    }

    /**
     * 查询授权管理对象 sys_client列表
     */
    @Override
    public List<SysClientVo> queryList(SysClientQuery query) {
        List<SysClientVo> list = MapstructUtil.convert(buildQueryChain(query).list(), SysClientVo.class);
        list.forEach(this::fillRuleFields);
        return list;
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysClient> buildQueryChain(SysClientQuery query) {
        return QueryChain.of(sysClientMapper)
                .forSearch(true)
                .where(query);
    }

    /**
     * 新增授权管理对象 sys_client
     */
    @Override
    public Boolean insertByBo(SysClientBo bo) {
        SysClient sysClient = MapstructUtil.convert(bo, SysClient.class);
        sysClient.setGrantType(resolveList(bo.getGrantType(), bo.getGrantTypeList(), false));
        sysClient.setAccessPath(resolveList(bo.getAccessPath(), bo.getAccessPathList(), true));
        sysClient.setIpWhitelist(resolveList(bo.getIpWhitelist(), bo.getIpWhitelistList(), false));
        sysClient.setClientId(SecureUtil.md5(bo.getClientKey() + bo.getClientSecret()));
        if (sysClient.getActiveTimeout() == null) {
            sysClient.setActiveTimeout(-1L);
        }
        if (sysClient.getTimeout() == null) {
            sysClient.setTimeout(604800L);
        }
        if (StringUtil.isBlank(sysClient.getStatus())) {
            sysClient.setStatus("0");
        }
        boolean flag = sysClientMapper.save(sysClient) > 0;
        bo.setId(sysClient.getId());
        return flag;
    }

    /**
     * 修改授权管理对象 sys_client
     */
    @Override
    public Boolean updateByBo(SysClientBo bo) {
        SysClient sysClient = MapstructUtil.convert(bo, SysClient.class);
        sysClient.setGrantType(resolveList(bo.getGrantType(), bo.getGrantTypeList(), false));
        sysClient.setAccessPath(resolveList(bo.getAccessPath(), bo.getAccessPathList(), true));
        sysClient.setIpWhitelist(resolveList(bo.getIpWhitelist(), bo.getIpWhitelistList(), false));
        return sysClientMapper.update(sysClient) > 0;
    }

    /**
     * 修改状态
     *
     * @param id 主键
     * @param status 状态
     */
    @Override
    public boolean updateUserStatus(Long id, String status) {
        return sysClientMapper.update(new SysClient().setId(id).setStatus(status)) > 0;
    }

    @Override
    public boolean updateClientStatus(String clientId, String status) {
        return sysClientMapper.update(new SysClient().setStatus(status),
                where -> where.eq(SysClient::getClientId, clientId)) > 0;
    }

    @Override
    public boolean checkClientKeyUnique(SysClientBo bo) {
        return !QueryChain.of(sysClientMapper)
                .where(where -> where.eq(SysClient::getClientKey, bo.getClientKey())
                        .ne(bo.getId() != null, SysClient::getId, bo.getId()))
                .exists();
    }

    /**
     * 批量删除授权管理对象 sys_client
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysClientMapper.deleteByIds(ids);
    }

    private SysClientVo fillRuleFields(SysClientVo vo) {
        if (vo == null) {
            return null;
        }
        vo.setGrantTypeList(parseList(vo.getGrantType(), false));
        vo.setAccessPathList(parseList(vo.getAccessPath(), true));
        vo.setIpWhitelistList(parseList(vo.getIpWhitelist(), false));
        return vo;
    }

    private String resolveList(String rawValue, List<String> listValue, boolean normalizePath) {
        List<String> values = rawValue != null
                ? StringUtil.str2List(rawValue, CLIENT_RULE_SEPARATOR_REGEX, true, true)
                : listValue;
        if (values == null) {
            return null;
        }
        return String.join(",", values.stream()
                .map(value -> normalizePath ? normalizePath(value) : StringUtil.trim(value))
                .filter(StringUtil::isNotBlank)
                .distinct()
                .toList());
    }

    private List<String> parseList(String value, boolean normalizePath) {
        return StringUtil.str2List(value, CLIENT_RULE_SEPARATOR_REGEX, true, true).stream()
                .map(item -> normalizePath ? normalizePath(item) : item)
                .filter(StringUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private String normalizePath(String path) {
        String value = StringUtil.trim(path);
        if (StringUtil.isBlank(value)) {
            return null;
        }
        if ("*".equals(value) || "/**".equals(value)) {
            return "/**";
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}
