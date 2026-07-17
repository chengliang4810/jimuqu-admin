package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.sse.utils.SseMessageUtil;
import com.jimuqu.system.domain.SysNotice;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.bo.SysNoticeBo;
import com.jimuqu.system.domain.query.SysNoticeQuery;
import com.jimuqu.system.domain.vo.SysMessageBoxVo;
import com.jimuqu.system.domain.vo.SysMessageVo;
import com.jimuqu.system.domain.vo.SysNoticeVo;
import com.jimuqu.system.mapper.SysNoticeMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * 通知公告与消息盒子服务。
 */
@Component
@RequiredArgsConstructor
public class SysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysUserMapper userMapper;

    public Page<SysNoticeVo> queryPage(SysNoticeQuery query, PageQuery pageQuery) {
        Page<SysNoticeVo> page = buildQuery(query)
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
        int rows = noticeMapper.save(notice);
        bo.setNoticeId(notice.getNoticeId());
        if (rows > 0 && "0".equals(notice.getStatus())) {
            SseMessageUtil.sendPayload(toMessage(toVo(notice)));
        }
        return rows;
    }

    public int update(SysNoticeBo bo) {
        return noticeMapper.update(toEntity(bo));
    }

    public int delete(List<Long> ids) {
        return noticeMapper.deleteByIds(ids);
    }

    public SysMessageBoxVo queryMessageBox() {
        List<SysNoticeVo> notices = QueryChain.of(noticeMapper)
                .where(where -> where.eq(SysNotice::getStatus, "0"))
                .orderBy(SysNotice::getNoticeId)
                .returnType(SysNoticeVo.class)
                .list();
        Collections.reverse(notices);
        SysMessageBoxVo box = new SysMessageBoxVo();
        box.setNoticeList(notices.stream().limit(20).map(this::toMessage).toList());
        return box;
    }

    private QueryChain<SysNotice> buildQuery(SysNoticeQuery query) {
        QueryChain<SysNotice> chain = QueryChain.of(noticeMapper).forSearch(true).where(query);
        if (StringUtil.isNotBlank(query.getCreateByName())) {
            SysUser user = QueryChain.of(userMapper)
                    .where(where -> where.eq(SysUser::getUserName, query.getCreateByName()))
                    .get();
            chain.where(where -> where.eq(SysNotice::getCreateBy, user == null ? -1L : user.getId()));
        }
        return chain.orderBy(SysNotice::getNoticeId);
    }

    private SysNotice toEntity(SysNoticeBo bo) {
        return new SysNotice()
                .setNoticeId(bo.getNoticeId())
                .setNoticeTitle(bo.getNoticeTitle())
                .setNoticeType(bo.getNoticeType())
                .setNoticeContent(bo.getNoticeContent())
                .setStatus(bo.getStatus() == null ? "0" : bo.getStatus())
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

    private SysMessageVo toMessage(SysNoticeVo notice) {
        String typeLabel = "2".equals(notice.getNoticeType()) ? "公告" : "通知";
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
                .setMessageId(String.valueOf(notice.getNoticeId()))
                .setCategory("notice")
                .setType("notice")
                .setSource("notice")
                .setTitle("[" + typeLabel + "] " + notice.getNoticeTitle())
                .setMessage(notice.getNoticeTitle())
                .setContent(notice.getNoticeContent())
                .setData(data)
                .setPath("/system/notice?noticeId=" + notice.getNoticeId())
                .setTimestamp(BigDecimal.valueOf(timestamp))
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
                .where(where -> where.in(SysUser::getId, userIds))
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
