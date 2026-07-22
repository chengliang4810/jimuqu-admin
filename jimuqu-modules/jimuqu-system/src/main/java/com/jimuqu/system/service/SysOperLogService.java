package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.utils.ip.AddressUtil;
import com.jimuqu.common.log.event.OperLogEvent;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
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
        QueryChain<SysOperLog> chain = pageQuery.applyOrder(buildQuery(query),
                queryChain -> queryChain.orderByDesc(SysOperLog::getOperId));
        return chain.returnType(SysOperLogVo.class).paging(pageQuery.build());
    }

    public List<SysOperLogVo> queryList(SysOperLogQuery query) {
        return buildQuery(query).orderByDesc(SysOperLog::getOperId).returnType(SysOperLogVo.class).list();
    }

    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Assert.isFalse(ids.stream().anyMatch(java.util.Objects::isNull), "操作日志ID不能为空");
        List<Long> requested = ids.stream().distinct().toList();
        long existing = QueryChain.of(mapper)
                .in(SysOperLog::getOperId, requested)
                .count();
        Assert.isTrue(existing == requested.size(), "操作日志不存在");
        return mapper.deleteByIds(requested);
    }

    public int clean() {
        return mapper.delete(where -> where.isNotNull(SysOperLog::getOperId));
    }

    public void record(OperLogEvent event) {
        mapper.save(toEntity(event));
    }

    static SysOperLog toEntity(OperLogEvent event) {
        return new SysOperLog()
                .setOperId(event.getOperId())
                .setTitle(event.getTitle())
                .setBusinessType(event.getBusinessType())
                .setMethod(event.getMethod())
                .setRequestMethod(event.getRequestMethod())
                .setOperatorType(event.getOperatorType())
                .setOperName(event.getOperName())
                .setUserId(event.getUserId())
                .setDeptId(event.getDeptId())
                .setDeptName(event.getDeptName())
                .setClientKey(event.getClientKey())
                .setDeviceType(event.getDeviceType())
                .setBrowser(event.getBrowser())
                .setOs(event.getOs())
                .setOperUrl(event.getOperUrl())
                .setOperIp(event.getOperIp())
                .setOperLocation(AddressUtil.getRealAddressByIP(event.getOperIp()))
                .setOperParam(event.getOperParam())
                .setJsonResult(event.getJsonResult())
                .setStatus(event.getStatus())
                .setErrorMsg(event.getErrorMsg())
                .setOperTime(event.getOperTime() == null ? new Date() : event.getOperTime())
                .setCostTime(event.getCostTime());
    }

    private QueryChain<SysOperLog> buildQuery(SysOperLogQuery query) {
        return QueryChain.of(mapper).forSearch(true).where(query);
    }
}
