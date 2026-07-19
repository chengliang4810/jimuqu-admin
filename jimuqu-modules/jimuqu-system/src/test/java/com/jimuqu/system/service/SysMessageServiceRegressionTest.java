package com.jimuqu.system.service;

import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.system.domain.SysMessage;
import com.jimuqu.system.domain.vo.SysMessageBoxVo;
import com.jimuqu.system.mapper.SysMessageMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SysMessageServiceRegressionTest {

    @Test
    @SuppressWarnings("unchecked")
    void messageBoxUsesDatabaseLimitAndMapsBlankDataJsonToNull() {
        List<Page<?>> requestedPages = new ArrayList<>();
        SysMessage row = new SysMessage()
                .setMessageId(42L)
                .setCategory("system")
                .setType("message")
                .setDataJson("   ");
        row.setCreateTime(new Date(1_750_000_000_123L));
        SysMessageMapper mapper = (SysMessageMapper) Proxy.newProxyInstance(
                SysMessageMapper.class.getClassLoader(), new Class<?>[]{SysMessageMapper.class},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return SysMessage.class;
                    }
                    if ("paging".equals(method.getName())) {
                        Page<SysMessage> page = (Page<SysMessage>) args[1];
                        requestedPages.add(page);
                        page.setRows(List.of(row));
                        return page;
                    }
                    return primitiveDefault(method.getReturnType());
                });

        SysMessageBoxVo box = new SysMessageService(mapper).queryMessageBox(7L);

        assertEquals(2, requestedPages.size(), "系统消息和通知消息都必须在数据库层分页");
        requestedPages.forEach(page -> {
            assertEquals(1, page.getCurrentPage());
            assertEquals(100, page.getPageSize());
            assertFalse(page.getExecuteCount(), "消息盒子只取第一页，不应额外执行 count");
        });
        assertNull(box.getSystemList().get(0).getData());
        assertNull(box.getNoticeList().get(0).getData());
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        return 0;
    }
}
