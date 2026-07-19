package com.jimuqu.system.runner;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoTableInitDataTest {

    @Test
    void productionKeepsFreshDatabaseInitializationEnabled() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app-prod.yml")) {
            assertNotNull(input);
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("auto-table:"));
            assertTrue(config.contains("  mode: update"));
            assertFalse(config.contains("  mode: validate"));
            assertTrue(config.contains("${JIMU_DB_URL:jdbc:mysql://127.0.0.1:3306/jimuqu_db"));
            assertTrue(config.contains("${JIMU_DB_USERNAME}"));
            assertTrue(config.contains("${JIMU_DB_PASSWORD}"));
            assertFalse(config.contains("${JIMU_DB_USERNAME:root}"));
            assertFalse(config.contains("${JIMU_DB_PASSWORD:P@ssw0rd}"));
            assertFalse(config.contains("jdbc:mysql://mariadb:"));
        }
    }

    @Test
    void defaultRuntimeEnablesRedisBackedBellFeatures() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.yml")) {
            assertNotNull(input);
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("driverType: \"redis\""));
            assertTrue(config.contains("${JIMU_REDIS_SERVER:127.0.0.1:6379}"));
            assertTrue(config.contains("${JIMU_REDIS_DB:0}"));
            assertTrue(config.contains("${JIMU_REDIS_PREFIX:jimuqu}"));
        }
    }

    @Test
    void mysqlSeedScriptIsPackagedAndParseable() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.yml")) {
            assertNotNull(input);
            String config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("base-path: classpath:sql/{dialect}"));
            assertTrue(config.contains("default-init-file-name: default"));
            assertTrue(config.contains("table-default-charset: utf8mb4"));
            assertTrue(config.contains("table-default-collation: utf8mb4_unicode_ci"));
            assertFalse(config.contains("default-init-file-name: jimuqu"));
        }

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sql/MySQL/jimuqu.sql")) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(110, CCJSqlParserUtil.parseStatements(sql).getStatements().size());
            assertEquals(58, sql.split("'F','0','0','", -1).length - 1);
            assertEquals(75, sql.split("NULL,'N','Y','", -1).length - 1);
            assertEquals(1, sql.split("INSERT INTO `sys_oss_config`", -1).length - 1);
            assertEquals(2, sql.split("INSERT INTO `sys_client`", -1).length - 1);
            assertEquals(4, sql.split("INSERT INTO `sys_post`", -1).length - 1);
            assertEquals(4, Pattern.compile("INSERT INTO `sys_post` [^;]+,'',103,1,CURRENT_TIMESTAMP,NULL,NULL\\);")
                    .matcher(sql).results().count(), "岗位种子必须包含管理员创建部门与创建人");
            assertEquals(6, Pattern.compile("INSERT INTO `sys_role` [^;]+,NULL,103,1,CURRENT_TIMESTAMP,NULL,NULL\\);")
                    .matcher(sql).results().count(), "角色种子必须包含管理员创建部门与创建人");
            assertEquals(148, Pattern.compile(",103,1,CURRENT_TIMESTAMP,NULL,NULL\\)")
                    .matcher(sql).results().count(), "管理员创建的种子必须保留完整审计字段");
            assertEquals(1, Pattern.compile("INSERT INTO `sys_user` [^;]+'self_user'[^;]+,104,5,CURRENT_TIMESTAMP,NULL,NULL\\);")
                    .matcher(sql).results().count(), "仅本人测试用户必须由自己创建");
            assertFalse(sql.contains("2026-07-18 11:22:36"), "初始化时间不得固定为生成 SQL 的日期");
            assertFalse(Pattern.compile(",CURRENT_TIMESTAMP,(?:NULL,')").matcher(sql).find(),
                    "更新人为空时更新时间也必须为空");
            assertEquals(2, sql.split("INSERT INTO `sys_notice`", -1).length - 1);
            assertTrue(sql.contains("'pc','pc123','password,sms,email,social','pc'"));
            assertTrue(sql.contains("'app','app123','password,sms,social','android','/app/**'"));
            assertTrue(sql.contains("'sys.user.initPassword','123456','Y'"));
            assertTrue(sql.contains("'admin@jimuqu.local','15888888888'"));
            assertTrue(sql.contains("(103,101,'0,100,101','研发部',1,1,NULL,NULL"),
                    "研发部负责人必须为管理员");
            assertTrue(sql.contains("INSERT INTO `sys_role_dept` (`role_id`, `dept_id`) VALUES (2,104)"),
                    "自定义数据范围必须与本部门范围使用不同部门种子");
            assertFalse(sql.contains("INSERT INTO `sys_role_dept` (`role_id`, `dept_id`) VALUES (2,103)"));
            assertEquals(7, sql.split("'sys_user'", -1).length - 1);
            assertFalse(sql.contains("'pc_user'"));
            assertTrue(sql.contains("(1,0,1,'正常','0','sys_normal_disable','','primary','Y'"));
            assertTrue(sql.contains("(3,0,1,'成功','0','sys_common_status'"));
            assertTrue(sql.contains("(9,0,1,'男','0','sys_user_gender','','','Y'"));
            assertTrue(sql.contains("'password','sys_grant_type','el-check-tag','default'"));
            assertTrue(sql.contains("'小程序','xcx','sys_device_type','','default'"));
            assertFalse(sql.contains("'mini_program'"));
            assertTrue(sql.contains("'default',NULL,NULL,NULL,'',NULL,NULL,'N','', 'Y'"));
            assertFalse(sql.contains("NULL,'1','0','"));
            assertFalse(sql.contains("'生成代码'"));
            assertFalse(sql.contains("'资源管理'"));
            assertTrue(sql.contains("(108,1,'日志管理',9,'log','',NULL,'N','Y','M','0','0','','lucide:logs'"));
            assertTrue(sql.contains("(201,108,'操作日志',1,'operlog'"));
            assertTrue(sql.contains("(202,108,'登录日志',2,'logininfo'"));
            assertTrue(sql.contains("(109,1,'客户端管理',11,'client','system/client/index',NULL,'N','Y','C','0','0','system:client:list','material-symbols:logo-dev-outline'"));
            assertTrue(sql.contains("(105,1,'字典管理',6,'dict','system/dict/index'"),
                    "字典菜单必须加载包含类型与数据面板的 Bell 页面");
            assertFalse(sql.contains("'system/dict/type/index'"));
            assertTrue(sql.contains("(200,2,'在线用户',1,'online','monitor/online/index',NULL,'N','Y','C','0','0','monitor:online:list','solar:monitor-smartphone-outline'"));
            assertTrue(sql.contains("(300,1,'文件管理',10,'oss','system/oss/index'"));
            assertTrue(sql.contains("(301,1,'文件配置管理',10,'oss-config/index','system/oss/config',NULL,'N','N','C'"));

            int menuStart = sql.indexOf("INSERT INTO `sys_menu`");
            int menuEnd = sql.indexOf("INSERT INTO `sys_oss_config`");
            Matcher menuMatcher = Pattern.compile("(?:VALUES\\s*)?\\((\\d+),(\\d+),'")
                    .matcher(sql.substring(menuStart, menuEnd));
            Set<Long> menuIds = new HashSet<>();
            List<Long> parentIds = new ArrayList<>();
            while (menuMatcher.find()) {
                assertTrue(menuIds.add(Long.parseLong(menuMatcher.group(1))), "菜单 ID 不得重复");
                parentIds.add(Long.parseLong(menuMatcher.group(2)));
            }
            assertEquals(76, menuIds.size());
            assertEquals(2, parentIds.stream().filter(parentId -> parentId == 0L).count());
            assertTrue(menuIds.containsAll(parentIds.stream().filter(parentId -> parentId != 0L).toList()),
                    "所有菜单父节点必须存在");
        }
    }
}
