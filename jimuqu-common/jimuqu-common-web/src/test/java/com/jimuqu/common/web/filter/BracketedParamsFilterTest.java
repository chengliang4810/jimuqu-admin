package com.jimuqu.common.web.filter;

import com.jimuqu.common.core.utils.JsonUtil;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ContextEmpty;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketedParamsFilterTest {

    @Test
    void aggregatesOnlySpringStyleParamsWithoutRemovingOriginalValues() throws Throwable {
        Context context = ContextEmpty.create();
        context.paramMap().put("params[beginTime]", "2026-07-01 00:00:00");
        context.paramMap().put("params[endTime]", "2026-07-31 23:59:59");
        context.paramMap().put("roleIds", "1");
        context.paramMap().add("roleIds", "2");
        AtomicBoolean continued = new AtomicBoolean();

        new BracketedParamsFilter().doFilter(context, ctx -> continued.set(true));

        assertTrue(continued.get());
        assertEquals("1", context.param("roleIds"));
        assertEquals(2, context.paramValues("roleIds").length);
        assertEquals("2026-07-01 00:00:00", context.param("params[beginTime]"));
        assertEquals(Map.of(
                        "beginTime", "2026-07-01 00:00:00",
                        "endTime", "2026-07-31 23:59:59"),
                JsonUtil.toObject(context.param("params"), Map.class));
    }

    @Test
    void preservesExplicitParamsValue() throws Throwable {
        Context context = ContextEmpty.create();
        context.paramMap().put("params", "{\"source\":\"explicit\"}");
        context.paramMap().put("params[source]", "bracketed");

        new BracketedParamsFilter().doFilter(context, ctx -> { });

        assertEquals("{\"source\":\"explicit\"}", context.param("params"));
    }
}
