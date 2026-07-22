package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.service.DictService;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysNotice;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.bo.SysNoticeBo;
import com.jimuqu.system.domain.query.SysNoticeQuery;
import com.jimuqu.system.domain.vo.SysMessageVo;
import com.jimuqu.system.domain.vo.SysNoticeVo;
import com.jimuqu.system.mapper.SysNoticeMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知公告与消息盒子服务。
 */
@Component
@RequiredArgsConstructor
public class SysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysUserMapper userMapper;
    private final SysMessageService messageService;
    private final DictService dictService;

    public Page<SysNoticeVo> queryPage(SysNoticeQuery query, PageQuery pageQuery) {
        Page<SysNoticeVo> page = pageQuery.applyOrder(buildQuery(query))
                .returnType(SysNoticeVo.class)
                .paging(pageQuery.build());
        fillCreatorNames(page.getRows());
        return page;
    }

    public SysNoticeVo queryById(Long noticeId) {
        SysNoticeVo vo = noticeMapper.getVoById(noticeId);
        fillCreatorNames(vo == null ? List.of() : List.of(vo));
        return vo;
    }

    public int insert(SysNoticeBo bo) {
        SysNotice notice = toEntity(bo);
        if (notice.getStatus() == null) {
            notice.setStatus("0");
        }
        int rows = noticeMapper.save(notice);
        bo.setNoticeId(notice.getNoticeId());
        if (rows > 0) {
            messageService.publishNotice(toMessage(toVo(notice)));
        }
        return rows;
    }

    public int update(SysNoticeBo bo) {
        return noticeMapper.update(toEntity(bo));
    }

    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Assert.isFalse(ids.stream().anyMatch(Objects::isNull), "公告ID不能为空");
        List<Long> requested = ids.stream().distinct().toList();
        long existing = QueryChain.of(noticeMapper)
                .in(SysNotice::getNoticeId, requested)
                .count();
        Assert.isTrue(existing == requested.size(), "通知公告不存在");
        return noticeMapper.deleteByIds(requested);
    }

    private QueryChain<SysNotice> buildQuery(SysNoticeQuery query) {
        QueryChain<SysNotice> chain = QueryChain.of(noticeMapper).forSearch(true).where(query);
        if (StringUtil.isNotBlank(query.getCreateByName())) {
            SysUser user = QueryChain.of(userMapper)
                    .eq(SysUser::getUserName, query.getCreateByName())
                    .get();
            chain.eq(SysNotice::getCreateBy, user == null ? -1L : user.getId());
        }
        return chain.orderBy(SysNotice::getNoticeId);
    }

    private SysNotice toEntity(SysNoticeBo bo) {
        return new SysNotice()
                .setNoticeId(bo.getNoticeId())
                .setNoticeTitle(bo.getNoticeTitle())
                .setNoticeType(bo.getNoticeType())
                .setNoticeContent(bo.getNoticeContent())
                .setStatus(bo.getStatus())
                .setRemark(bo.getRemark());
    }

    private SysNoticeVo toVo(SysNotice notice) {
        return new SysNoticeVo()
                .setNoticeId(notice.getNoticeId())
                .setNoticeTitle(notice.getNoticeTitle())
                .setNoticeType(notice.getNoticeType())
                .setNoticeContent(notice.getNoticeContent())
                .setStatus(notice.getStatus())
                .setRemark(notice.getRemark())
                .setCreateBy(notice.getCreateBy())
                .setCreateTime(notice.getCreateTime());
    }

    SysMessageVo toMessage(SysNoticeVo notice) {
        String typeLabel = dictService.getDictLabel("sys_notice_type", notice.getNoticeType());
        long timestamp = notice.getCreateTime() == null
                ? System.currentTimeMillis()
                : notice.getCreateTime().getTime();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("noticeId", notice.getNoticeId());
        data.put("noticeTitle", notice.getNoticeTitle());
        data.put("noticeType", notice.getNoticeType());
        data.put("noticeTypeLabel", typeLabel);
        data.put("noticeContent", notice.getNoticeContent());
        data.put("status", notice.getStatus());
        return new SysMessageVo()
                .setType("notice")
                .setSource("notice")
                .setMessage("[" + typeLabel + "] " + notice.getNoticeTitle())
                .setData(data)
                .setPath("/system/notice?noticeId=" + notice.getNoticeId())
                .setTimestamp(timestamp)
                .setCreateTime(notice.getCreateTime());
    }

    private void fillCreatorNames(List<SysNoticeVo> notices) {
        List<Long> userIds = notices.stream()
                .map(SysNoticeVo::getCreateBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> users = QueryChain.of(userMapper)
                .select(SysUser::getId, SysUser::getUserName)
                .in(SysUser::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUserName, (left, right) -> left));
        notices.forEach(notice -> {
            String userName = users.get(notice.getCreateBy());
            if (userName != null) {
                notice.setCreateByName(userName);
            }
        });
    }
}
