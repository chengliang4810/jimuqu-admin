package com.jimuqu.system.service;

import cn.hutool.v7.http.useragent.UserAgent;
import cn.hutool.v7.http.useragent.UserAgentUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysClient;
import com.jimuqu.system.domain.SysLoginInfo;
import com.jimuqu.system.domain.query.SysLoginInfoQuery;
import com.jimuqu.system.domain.vo.SysLoginInfoVo;
import com.jimuqu.system.mapper.SysLoginInfoMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.Date;
import java.util.List;

/**
 * 登录日志服务。
 */
@Component
@RequiredArgsConstructor
public class SysLoginInfoService {

    private final SysLoginInfoMapper mapper;
    private final SysClientService clientService;

    public Page<SysLoginInfoVo> queryPage(SysLoginInfoQuery query, PageQuery pageQuery) {
        return pageQuery.applyOrder(buildQuery(query),
                        chain -> chain.orderByDesc(SysLoginInfo::getInfoId))
                .returnType(SysLoginInfoVo.class).paging(pageQuery.build());
    }

    public List<SysLoginInfoVo> queryList(SysLoginInfoQuery query) {
        return buildQuery(query).orderByDesc(SysLoginInfo::getInfoId)
                .returnType(SysLoginInfoVo.class).list();
    }

    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Assert.isFalse(ids.stream().anyMatch(java.util.Objects::isNull), "登录日志ID不能为空");
        List<Long> requested = ids.stream().distinct().toList();
        long existing = QueryChain.of(mapper)
                .where(where -> where.in(SysLoginInfo::getInfoId, requested))
                .count();
        Assert.isTrue(existing == requested.size(), "登录日志不存在");
        return mapper.deleteByIds(requested);
    }

    public int clean() {
        return mapper.delete(where -> where.isNotNull(SysLoginInfo::getInfoId));
    }

    public void record(LogininforEvent event) {
        SysLoginInfo entity = new SysLoginInfo()
                .setUserName(event.getUsername())
                .setStatus(isSuccess(event.getStatus()) ? Constants.SUCCESS : Constants.FAIL)
                .setMsg(event.getMessage())
                .setLoginTime(new Date());
        if (StringUtil.isNotBlank(event.getIpaddr())) {
            entity.setIpaddr(event.getIpaddr())
                    .setLoginLocation(AddressUtil.getRealAddressByIP(event.getIpaddr()));
        }
        if (StringUtil.isNotBlank(event.getUserAgent())) {
            UserAgent agent = UserAgentUtil.parse(event.getUserAgent());
            entity.setBrowser(agent.getBrowser().getName()).setOs(agent.getOs().getName());
        }
        if (StringUtil.isNotBlank(event.getClientId())) {
            SysClient client = clientService.queryByClientId(event.getClientId());
            if (client != null) {
                entity.setClientKey(client.getClientKey()).setDeviceType(client.getDeviceType());
            }
        }
        mapper.save(entity);
    }

    private boolean isSuccess(String status) {
        return Constants.LOGIN_SUCCESS.equals(status)
                || Constants.LOGOUT.equals(status)
                || Constants.REGISTER.equals(status);
    }

    private QueryChain<SysLoginInfo> buildQuery(SysLoginInfoQuery query) {
        return QueryChain.of(mapper).forSearch(true).where(query);
    }
}
