package com.jimuqu.system.service;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.redis.utils.RedisUtils;
import com.jimuqu.common.sse.dto.SseMessageDto;
import com.jimuqu.common.sse.utils.SseMessageUtil;
import com.jimuqu.common.websocket.holder.WebSocketSessionHolder;
import com.jimuqu.system.domain.SysMessage;
import com.jimuqu.system.domain.vo.SysMessageBoxVo;
import com.jimuqu.system.domain.vo.SysMessageVo;
import com.jimuqu.system.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.annotation.Init;
import org.noear.solon.cache.redisson.RedissonCacheService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SysMessageService {

    private static final String GLOBAL_USER_IDS = "0";
    private static final long BOX_DAYS = 30L;
    private static final int BOX_LIMIT = 100;
    private static final String MESSAGE_TOPIC = "global:message";
    private static final String CATEGORY_SYSTEM = "system";
    private static final String CATEGORY_NOTICE = "notice";

    private final SysMessageMapper mapper;
    private volatile Integer topicListenerId;
    private volatile boolean destroyed;

    @Init
    public void registerMessageTopic() {
        Solon.context().getBeanAsync(RedissonCacheService.class, ignored -> subscribeMessageTopic());
    }

    @Destroy
    public synchronized void destroyMessageTopic() {
        destroyed = true;
        if (topicListenerId != null) {
            RedisUtils.unsubscribe(MESSAGE_TOPIC, topicListenerId);
            topicListenerId = null;
        }
    }

    private synchronized void subscribeMessageTopic() {
        if (!destroyed && topicListenerId == null) {
            topicListenerId = RedisUtils.subscribe(MESSAGE_TOPIC, SseMessageDto.class, this::dispatch);
        }
    }

    public void publishNotice(SysMessageVo payload) {
        publishAll(payload);
    }

    public void publishAll(SysMessageVo payload) {
        publishMessage(List.of(), payload);
    }

    public void publishMessage(List<Long> userIds, SysMessageVo payload) {
        if (payload == null) {
            return;
        }
        List<Long> targets = userIds == null ? List.of() : userIds.stream()
                .filter(Objects::nonNull).distinct().toList();
        if (supportsMessageBox(payload)) {
            SysMessage entity = buildMessage(targets, payload);
            mapper.save(entity);
            payload.setMessageId(String.valueOf(entity.getMessageId()));
        }
        if (payload.getTimestamp() == null) {
            payload.setTimestamp(System.currentTimeMillis());
        }
        SseMessageDto message = new SseMessageDto();
        message.setUserIds(targets);
        message.setMessage(JsonUtil.toString(payload));
        RedisUtils.publish(MESSAGE_TOPIC, message);
    }

    private SysMessage buildMessage(List<Long> targets, SysMessageVo payload) {
        return new SysMessage()
                .setCategory(resolveCategory(payload)).setType(payload.getType()).setSource(payload.getSource())
                .setTitle(resolveTitle(payload)).setMessage(payload.getMessage()).setContent(resolveContent(payload))
                .setDataJson(JsonUtil.toString(payload.getData())).setPath(payload.getPath())
                .setSendUserIds(targets.isEmpty() ? GLOBAL_USER_IDS : targets.stream()
                        .map(String::valueOf).collect(Collectors.joining(",")));
    }

    private boolean supportsMessageBox(SysMessageVo payload) {
        return "message".equals(payload.getType()) || "notice".equals(payload.getType());
    }

    private String resolveCategory(SysMessageVo payload) {
        return "notice".equals(payload.getType()) || "notice".equals(payload.getSource())
                ? CATEGORY_NOTICE : CATEGORY_SYSTEM;
    }

    private String resolveTitle(SysMessageVo payload) {
        return CATEGORY_NOTICE.equals(resolveCategory(payload)) ? "通知公告消息" : "系统消息";
    }

    private String resolveContent(SysMessageVo payload) {
        if (payload.getData() instanceof Map<?, ?> data) {
            Object content = data.get("noticeContent");
            return content == null ? null : content.toString();
        }
        return null;
    }

    private void dispatch(SseMessageDto message) {
        if (message == null || message.getMessage() == null) {
            return;
        }
        List<Long> userIds = message.getUserIds();
        if (userIds == null || userIds.isEmpty()) {
            SseMessageUtil.sendPayload(JsonUtil.toObject(message.getMessage(), Object.class));
            WebSocketSessionHolder.sendAll(message.getMessage());
            return;
        }
        userIds.forEach(userId -> {
            SseMessageUtil.sendPayload(userId, JsonUtil.toObject(message.getMessage(), Object.class));
            WebSocketSessionHolder.sendMessage(userId, message.getMessage());
        });
    }

    public SysMessageBoxVo queryMessageBox(Long userId) {
        SysMessageBoxVo box = new SysMessageBoxVo();
        box.setSystemList(select(CATEGORY_SYSTEM, userId));
        box.setNoticeList(select(CATEGORY_NOTICE, userId));
        return box;
    }

    private List<SysMessageVo> select(String category, Long userId) {
        Date cutoff = new Date(System.currentTimeMillis() - BOX_DAYS * 24 * 60 * 60 * 1000);
        QueryChain<SysMessage> query = QueryChain.of(mapper)
                .eq(SysMessage::getCategory, category)
                .gte(SysMessage::getCreateTime, cutoff)
                .orderByDesc(SysMessage::getCreateTime, SysMessage::getMessageId);
        query.andNested(scope -> scope.eq(SysMessage::getSendUserIds, GLOBAL_USER_IDS)
                .or(SysMessage::getSendUserIds, value -> value.mysql().findInSet(userId)));
        Page<SysMessage> page = query.paging(Page.<SysMessage>of(1, BOX_LIMIT).setExecuteCount(false));
        return page.getRows().stream().map(this::toVo).toList();
    }

    private SysMessageVo toVo(SysMessage entity) {
        Date createTime = entity.getCreateTime();
        return new SysMessageVo().setMessageId(String.valueOf(entity.getMessageId()))
                .setCategory(entity.getCategory()).setType(entity.getType()).setSource(entity.getSource())
                .setTitle(entity.getTitle()).setMessage(entity.getMessage()).setContent(entity.getContent())
                .setData(parseData(entity.getDataJson())).setPath(entity.getPath())
                .setTimestamp(createTime == null ? System.currentTimeMillis() : createTime.getTime())
                .setCreateTime(createTime);
    }

    private Object parseData(String dataJson) {
        return StringUtil.isBlank(dataJson) ? null : JsonUtil.toObject(dataJson, Object.class);
    }
}
