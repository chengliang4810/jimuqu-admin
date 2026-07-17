package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.sse.utils.SseMessageUtil;
import com.jimuqu.system.domain.SysMessage;
import com.jimuqu.system.domain.vo.SysMessageBoxVo;
import com.jimuqu.system.domain.vo.SysMessageVo;
import com.jimuqu.system.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SysMessageService {

    private static final String GLOBAL_USER_IDS = "0";
    private static final long BOX_DAYS = 30L;
    private static final int BOX_LIMIT = 100;

    private final SysMessageMapper mapper;

    public void publishNotice(SysMessageVo payload) {
        SysMessage entity = new SysMessage()
                .setCategory("notice").setType(payload.getType()).setSource(payload.getSource())
                .setTitle(payload.getTitle()).setMessage(payload.getMessage()).setContent(payload.getContent())
                .setDataJson(JsonUtil.toString(payload.getData())).setPath(payload.getPath())
                .setSendUserIds(GLOBAL_USER_IDS);
        mapper.save(entity);
        payload.setMessageId(String.valueOf(entity.getMessageId()));
        SseMessageUtil.sendPayload(payload);
    }

    public SysMessageBoxVo queryMessageBox() {
        SysMessageBoxVo box = new SysMessageBoxVo();
        box.setSystemList(select("system"));
        box.setNoticeList(select("notice"));
        return box;
    }

    private List<SysMessageVo> select(String category) {
        Date cutoff = new Date(System.currentTimeMillis() - BOX_DAYS * 24 * 60 * 60 * 1000);
        return QueryChain.of(mapper)
                .where(where -> where.eq(SysMessage::getCategory, category)
                        .eq(SysMessage::getSendUserIds, GLOBAL_USER_IDS)
                        .gte(SysMessage::getCreateTime, cutoff))
                .orderByDesc(SysMessage::getCreateTime, SysMessage::getMessageId)
                .list().stream().limit(BOX_LIMIT).map(this::toVo).toList();
    }

    private SysMessageVo toVo(SysMessage entity) {
        Date createTime = entity.getCreateTime();
        return new SysMessageVo().setMessageId(String.valueOf(entity.getMessageId()))
                .setCategory(entity.getCategory()).setType(entity.getType()).setSource(entity.getSource())
                .setTitle(entity.getTitle()).setMessage(entity.getMessage()).setContent(entity.getContent())
                .setData(JsonUtil.toObject(entity.getDataJson(), Object.class)).setPath(entity.getPath())
                .setTimestamp(BigDecimal.valueOf(createTime == null ? System.currentTimeMillis() : createTime.getTime()))
                .setCreateTime(createTime);
    }
}
