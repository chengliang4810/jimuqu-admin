package com.jimuqu.system.domain.vo;

import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.web.serialization.ForyJsonStringSerializer;
import com.jimuqu.common.web.sensitive.SensitiveJsonRender;
import com.jimuqu.system.domain.bo.SysMenuBo;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.handle.ContextEmpty;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouterJsonInclusionTest {
    @Test
    void omitsEmptyRouterPropertiesInUtilityAndHttpResponses() throws Throwable {
        RouterVo child = new RouterVo();
        child.setName("User");
        child.setPath("");
        child.setChildren(List.of());
        RouterVo parent = new RouterVo();
        parent.setName("System");
        parent.setChildren(List.of(child));
        ForyJsonStringSerializer serializer = new ForyJsonStringSerializer();
        for (String json : List.of(JsonUtil.toString(parent), serializer.serialize(parent),
                new SensitiveJsonRender(serializer).renderAndReturn(parent, ContextEmpty.create()))) {
            Map<?, ?> tree = JsonUtil.toObject(json, Map.class);
            assertFalse(tree.containsKey("path"));
            assertFalse(tree.containsKey("meta"));
            assertEquals(false, tree.get("hidden"));
            Map<?, ?> nested = (Map<?, ?>) ((List<?>) tree.get("children")).getFirst();
            assertEquals("User", nested.get("name"));
            assertFalse(nested.containsKey("path"));
            assertFalse(nested.containsKey("children"));
        }
    }

    @Test
    void omitsOnlyNullMenuPermissionsEvenWhenHttpWritesOtherNulls() {
        SysMenuBo menu = new SysMenuBo();
        ForyJsonStringSerializer serializer = new ForyJsonStringSerializer();
        Map<?, ?> tree = JsonUtil.toObject(serializer.serialize(menu), Map.class);
        assertFalse(tree.containsKey("perms"));
        assertTrue(tree.containsKey("menuName"));
        menu.setPerms("");
        tree = JsonUtil.toObject(serializer.serialize(menu), Map.class);
        assertEquals("", tree.get("perms"));
    }
}
