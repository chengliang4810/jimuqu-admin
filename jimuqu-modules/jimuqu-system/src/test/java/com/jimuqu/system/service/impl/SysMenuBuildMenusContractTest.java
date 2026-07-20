package com.jimuqu.system.service.impl;

import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.system.domain.SysMenu;
import com.jimuqu.system.domain.vo.RouterVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysMenuBuildMenusContractTest {

    private final SysMenuServiceImpl service = new SysMenuServiceImpl(null, null, null);

    @Test
    void returnsEmptyRoutesForMissingMenus() {
        assertTrue(service.buildMenus(null).isEmpty());
        assertTrue(service.buildMenus(List.of()).isEmpty());
    }

    @Test
    void buildsDirectoryTreeWithBellRouteFields() {
        SysMenu child = menu(11L, 10L, "用户管理", "user", UserConstants.TYPE_MENU)
                .setComponent("system/user/index")
                .setVisible("1")
                .setIsCache(UserConstants.NO)
                .setQueryParam("{\"id\":1}")
                .setExt("{\"hideInTab\":true}")
                .setActiveMenu("/system/user");
        SysMenu directory = menu(10L, 0L, "系统管理", "system", UserConstants.TYPE_DIR)
                .setIcon("eos-icons:system-group")
                .setChildren(List.of(child));

        RouterVo route = service.buildMenus(List.of(directory)).get(0);

        assertEquals("System10", route.getName());
        assertEquals("/system", route.getPath());
        assertEquals(UserConstants.LAYOUT, route.getComponent());
        assertFalse(route.isHidden());
        assertEquals(Boolean.TRUE, route.getAlwaysShow());
        assertEquals("noRedirect", route.getRedirect());
        assertEquals("系统管理", route.getMeta().getTitle());
        assertEquals("eos-icons:system-group", route.getMeta().getIcon());
        assertFalse(route.getMeta().isNoCache());
        assertNull(route.getMeta().getLink());

        RouterVo childRoute = route.getChildren().get(0);
        assertEquals("User11", childRoute.getName());
        assertEquals("user", childRoute.getPath());
        assertEquals("system/user/index", childRoute.getComponent());
        assertTrue(childRoute.isHidden());
        assertEquals("{\"id\":1}", childRoute.getQuery());
        assertEquals("{\"hideInTab\":true}", childRoute.getExt());
        assertTrue(childRoute.getMeta().isNoCache());
        assertEquals("/system/user", childRoute.getMeta().getActiveMenu());
        assertNull(childRoute.getMeta().getLink());
    }

    @Test
    void wrapsRootMenuForBellRootMenuConversion() {
        SysMenu menu = menu(20L, 0L, "概览", "dashboard", UserConstants.TYPE_MENU)
                .setComponent("dashboard/index")
                .setIsCache(UserConstants.NO)
                .setQueryParam("{\"tab\":\"workbench\"}")
                .setExt("{\"order\":1}")
                .setActiveMenu("/dashboard");

        RouterVo route = service.buildMenus(List.of(menu)).get(0);

        assertEquals("20", route.getName());
        assertEquals("/", route.getPath());
        assertEquals(UserConstants.LAYOUT, route.getComponent());
        assertNull(route.getMeta());
        assertNull(route.getAlwaysShow());
        assertNull(route.getRedirect());
        assertEquals(1, route.getChildren().size());

        RouterVo child = route.getChildren().get(0);
        assertEquals("Dashboard20", child.getName());
        assertEquals("dashboard", child.getPath());
        assertEquals("dashboard/index", child.getComponent());
        assertEquals("{\"tab\":\"workbench\"}", child.getQuery());
        assertEquals("{\"order\":1}", child.getExt());
        assertTrue(child.getMeta().isNoCache());
        assertEquals("/dashboard", child.getMeta().getActiveMenu());
    }

    @Test
    void buildsRootIframeLinkForBellInnerLinkConversion() {
        String url = "https://www.example.com/#/guide?lang=zh";
        SysMenu menu = menu(30L, 0L, "在线文档", url, UserConstants.TYPE_DIR)
                .setComponent("")
                .setIsCache(UserConstants.YES)
                .setExt("{\"openInNewWindow\":false}");

        RouterVo route = service.buildMenus(List.of(menu)).get(0);

        assertEquals("/", route.getPath());
        assertEquals("在线文档", route.getMeta().getTitle());
        assertEquals(1, route.getChildren().size());
        RouterVo child = route.getChildren().get(0);
        assertEquals("example/com/#/guide?lang=zh", child.getPath());
        assertEquals(UserConstants.INNER_LINK, child.getComponent());
        assertEquals("Example/com/#/guide?lang=zh30", child.getName());
        assertEquals(url, child.getMeta().getLink());
        assertEquals("{\"openInNewWindow\":false}", child.getExt());
        assertFalse(child.getMeta().isNoCache());
    }

    @Test
    void keepsIframeLinkAndQueryOnNestedInnerLink() {
        String url = "https://docs.example.com/start";
        SysMenu child = menu(41L, 40L, "嵌入文档", url, UserConstants.TYPE_MENU)
                .setComponent("")
                .setQueryParam("{\"locale\":\"zh-CN\"}")
                .setIsCache("1");
        SysMenu directory = menu(40L, 0L, "帮助", "help", UserConstants.TYPE_DIR)
                .setChildren(List.of(child));

        RouterVo route = service.buildMenus(List.of(directory)).get(0).getChildren().get(0);

        assertEquals("docs/example/com/start", route.getPath());
        assertEquals(UserConstants.INNER_LINK, route.getComponent());
        assertEquals(url, route.getMeta().getLink());
        assertEquals("{\"locale\":\"zh-CN\"}", route.getQuery());
        assertTrue(route.getMeta().isNoCache());
    }

    @Test
    void exposesExternalLinkWithoutIframeWrapper() {
        String url = "https://example.com/releases";
        SysMenu menu = menu(50L, 0L, "发布页", url, UserConstants.TYPE_MENU)
                .setIsFrame(UserConstants.YES)
                .setIsCache("0")
                .setQueryParam("{\"source\":\"menu\"}")
                .setExt("{\"openInNewWindow\":true}");

        RouterVo route = service.buildMenus(List.of(menu)).get(0);

        assertEquals(url, route.getPath());
        assertEquals(UserConstants.LAYOUT, route.getComponent());
        assertEquals(url, route.getMeta().getLink());
        assertEquals("{\"source\":\"menu\"}", route.getQuery());
        assertEquals("{\"openInNewWindow\":true}", route.getExt());
        assertFalse(route.getMeta().isNoCache());
        assertTrue(route.getChildren() == null || route.getChildren().isEmpty());
    }

    @Test
    void supportsCurrentAndLegacyCacheFlags() {
        assertFalse(SysMenuServiceImpl.isNoCache(UserConstants.YES));
        assertTrue(SysMenuServiceImpl.isNoCache(UserConstants.NO));
        assertFalse(SysMenuServiceImpl.isNoCache("0"));
        assertTrue(SysMenuServiceImpl.isNoCache("1"));
    }

    @Test
    void ignoresInvalidActiveMenuPathLikeRuoYiSix() {
        SysMenu menu = menu(60L, 1L, "隐藏详情", "detail", UserConstants.TYPE_MENU)
                .setComponent("system/detail/index")
                .setActiveMenu("system/user");

        RouterVo route = service.buildMenus(List.of(menu)).get(0);

        assertNull(route.getMeta().getActiveMenu());
    }

    private SysMenu menu(Long id, Long parentId, String name, String path, String type) {
        return new SysMenu()
                .setId(id)
                .setParentId(parentId)
                .setMenuName(name)
                .setPath(path)
                .setMenuType(type)
                .setComponent("")
                .setVisible("0")
                .setStatus("0")
                .setIcon("#")
                .setIsFrame(UserConstants.NO)
                .setIsCache(UserConstants.YES);
    }
}
