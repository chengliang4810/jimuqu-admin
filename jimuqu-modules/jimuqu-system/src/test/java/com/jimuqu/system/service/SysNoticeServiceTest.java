package com.jimuqu.system.service;

import com.jimuqu.common.core.service.DictService;
import com.jimuqu.system.domain.SysNotice;
import com.jimuqu.system.domain.bo.SysNoticeBo;
import com.jimuqu.system.domain.vo.SysMessageVo;
import com.jimuqu.system.domain.vo.SysNoticeVo;
import com.jimuqu.system.mapper.SysNoticeMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysNoticeServiceTest {

    @Test
    void mixedExistingAndMissingIdsDoNotDeleteAnyNotice() {
        AtomicBoolean deleteInvoked = new AtomicBoolean();
        SysNoticeMapper mapper = (SysNoticeMapper) Proxy.newProxyInstance(
                SysNoticeMapper.class.getClassLoader(), new Class<?>[]{SysNoticeMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysNotice.class;
                    }
                    if ("count".equals(method.getName())) {
                        return 1;
                    }
                    if ("deleteByIds".equals(method.getName())) {
                        deleteInvoked.set(true);
                        return 1;
                    }
                    return method.getReturnType().isPrimitive() ? 0 : null;
                });

        SysNoticeService service = new SysNoticeService(mapper, null, null, null);

        assertThrows(RuntimeException.class, () -> service.delete(List.of(1L, 999L)));
        assertFalse(deleteInvoked.get(), "存在任一无效 ID 时不得删除有效公告");
    }

    @Test
    void updateDoesNotReplaceMissingStatusWithNormal() {
        AtomicReference<SysNotice> updated = new AtomicReference<>();
        SysNoticeMapper mapper = (SysNoticeMapper) Proxy.newProxyInstance(
                SysNoticeMapper.class.getClassLoader(), new Class<?>[]{SysNoticeMapper.class},
                (proxy, method, args) -> {
                    if ("update".equals(method.getName()) && args != null && args[0] instanceof SysNotice notice) {
                        updated.set(notice);
                        return 1;
                    }
                    return method.getReturnType().isPrimitive() ? 0 : null;
                });
        SysNoticeService service = new SysNoticeService(mapper, null, null, null);
        SysNoticeBo notice = new SysNoticeBo();
        notice.setNoticeId(42L);
        notice.setNoticeTitle("编辑公告");

        assertEquals(1, service.update(notice));
        assertNull(updated.get().getStatus());
    }

    @Test
    void buildsNoticeMessageWithRuntimeDictionaryLabelAndNumericTimestamp() {
        DictService dictService = new DictService() {
            @Override
            public String getDictLabel(String dictType, String dictValue, String separator) {
                assertEquals("sys_notice_type", dictType);
                assertEquals("1", dictValue);
                return "运行时标签";
            }

            @Override
            public String getDictValue(String dictType, String dictLabel, String separator) {
                return "";
            }

            @Override
            public Map<String, String> getAllDictByDictType(String dictType) {
                return Map.of();
            }
        };
        SysNoticeService service = new SysNoticeService(null, null, null, dictService);
        Date createTime = new Date(1_750_000_000_123L);

        SysMessageVo message = service.toMessage(new SysNoticeVo()
                .setNoticeId(42L)
                .setNoticeTitle("动态字典通知")
                .setNoticeType("1")
                .setNoticeContent("内容")
                .setStatus("0")
                .setCreateTime(createTime));

        assertEquals("[运行时标签] 动态字典通知", message.getMessage());
        assertEquals(createTime.getTime(), message.getTimestamp());
        assertEquals("运行时标签", ((Map<?, ?>) message.getData()).get("noticeTypeLabel"));
    }
}
