package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.domain.model.LoginUser;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysOperLog;
import com.jimuqu.system.domain.query.SysOperLogQuery;
import com.jimuqu.system.domain.vo.SysOperLogVo;
import com.jimuqu.system.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.Date;
import java.util.List;

/**
 * 操作日志服务。
 */
@Component
@RequiredArgsConstructor
public class SysOperLogService {

    private final SysOperLogMapper mapper;

    public Page<SysOperLogVo> queryPage(SysOperLogQuery query, PageQuery pageQuery) {
        QueryChain<SysOperLog> chain = buildQuery(query);
        if (pageQuery.buildOrderBy().length == 0) {
            chain.orderByDesc(SysOperLog::getOperId);
        } else {
            chain.orderBy(pageQuery.buildOrderBy());
        }
        return chain.returnType(SysOperLogVo.class).paging(pageQuery.build());
    }

    public List<SysOperLogVo> queryList(SysOperLogQuery query) {
        return buildQuery(query).orderByDesc(SysOperLog::getOperId).returnType(SysOperLogVo.class).list();
    }

    public int delete(List<Long> ids) {
        return mapper.deleteByIds(ids);
    }

    public int clean() {
        return mapper.delete(where -> where.isNotNull(SysOperLog::getOperId));
    }

    public void record(OperLogEvent event) {
        SysOperLog entity = new SysOperLog()
                .setOperId(event.getOperId())
                .setTitle(event.getTitle())
                .setBusinessType(event.getBusinessType())
                .setMethod(event.getMethod())
                .setRequestMethod(event.getRequestMethod())
                .setOperatorType(event.getOperatorType())
                .setOperName(event.getOperName())
                .setDeptName(event.getDeptName())
                .setOperUrl(event.getOperUrl())
                .setOperIp(event.getOperIp())
                .setOperLocation(AddressUtil.getRealAddressByIP(event.getOperIp()))
                .setOperParam(event.getOperParam())
                .setJsonResult(event.getJsonResult())
                .setStatus(event.getStatus())
                .setErrorMsg(event.getErrorMsg())
                .setOperTime(event.getOperTime() == null ? new Date() : event.getOperTime())
                .setCostTime(event.getCostTime());
        try {
            LoginUser user = LoginHelper.getLoginUser();
            entity.setUserId(user.getUserId())
                    .setDeptId(user.getDeptId())
                    .setClientKey(user.getClientKey())
                    .setDeviceType(user.getDeviceType())
                    .setBrowser(user.getBrowser())
                    .setOs(user.getOs());
        } catch (RuntimeException ignored) {
        }
        mapper.save(entity);
    }

    private QueryChain<SysOperLog> buildQuery(SysOperLogQuery query) {
        return QueryChain.of(mapper).forSearch(true).where(query);
    }
}
