package com.jimuqu.system.service;

import cn.hutool.v7.http.useragent.UserAgent;
import cn.hutool.v7.http.useragent.UserAgentUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.constant.Constants;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import com.jimuqu.common.log.event.LogininforEvent;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysLoginInfo;
import com.jimuqu.system.domain.query.SysLoginInfoQuery;
import com.jimuqu.system.domain.vo.SysLoginInfoVo;
import com.jimuqu.system.mapper.SysLoginInfoMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;

import java.util.Date;
import java.util.List;

/**
 * 登录日志服务。
 */
@Component
@RequiredArgsConstructor
public class SysLoginInfoService {

    private final SysLoginInfoMapper mapper;

    public Page<SysLoginInfoVo> queryPage(SysLoginInfoQuery query, PageQuery pageQuery) {
        return buildQuery(query).orderByDesc(SysLoginInfo::getInfoId)
                .returnType(SysLoginInfoVo.class).paging(pageQuery.build());
    }

    public List<SysLoginInfoVo> queryList(SysLoginInfoQuery query) {
        return buildQuery(query).orderByDesc(SysLoginInfo::getInfoId)
                .returnType(SysLoginInfoVo.class).list();
    }

    public int delete(List<Long> ids) {
        return mapper.deleteByIds(ids);
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
        try {
            Context context = Context.current();
            String ip = context.realIp();
            UserAgent agent = UserAgentUtil.parse(context.header("User-Agent"));
            entity.setIpaddr(ip)
                    .setLoginLocation(AddressUtil.getRealAddressByIP(ip))
                    .setBrowser(agent.getBrowser().getName())
                    .setOs(agent.getOs().getName());
        } catch (RuntimeException ignored) {
        }
        try {
            LoginUser user = LoginHelper.getLoginUser();
            entity.setClientKey(user.getClientKey()).setDeviceType(user.getDeviceType());
        } catch (RuntimeException ignored) {
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
