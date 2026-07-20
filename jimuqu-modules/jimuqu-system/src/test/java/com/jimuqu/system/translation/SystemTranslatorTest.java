package com.jimuqu.system.translation;

import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.enums.TransType;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.vo.ProfileUserVo;
import com.jimuqu.system.domain.vo.SysNoticeVo;
import com.jimuqu.system.domain.vo.SysOssVo;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.domain.vo.SysUserVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.SysFileService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemTranslatorTest {

    @Test
    void translatesUserDeptAndOssIdentifiers() throws Exception {
        SysUser user = new SysUser().setId(7L).setUserName("admin").setNickName("管理员");
        SysDept dept = new SysDept().setId(8L).setDeptName("研发部");
        SysUserMapper userMapper = mapper(SysUserMapper.class, SysUser.class, user);
        SysDeptMapper deptMapper = mapper(SysDeptMapper.class, SysDept.class, dept);
        SysFileService fileService = (SysFileService) Proxy.newProxyInstance(
                SysFileService.class.getClassLoader(), new Class<?>[]{SysFileService.class},
                (proxy, method, args) -> "selectUrlByIds".equals(method.getName())
                        ? "/file/local-plus/avatar.png" : null);

        assertEquals("admin", new UserNameTranslator(userMapper).translate(7L, trans("userName")));
        assertEquals("管理员", new NicknameTranslator(userMapper).translate("7", trans("nickname")));
        assertEquals("研发部", new DeptNameTranslator(deptMapper).translate(8L, trans("deptName")));
        assertEquals("/file/local-plus/avatar.png",
                new OssUrlTranslator(fileService).translate(9L, trans("ossUrl")));
        assertEquals("未知", new UserNameTranslator(userMapper).translate("invalid", trans("userName")));
    }

    @Test
    void batchesUserDeptAndOssQueries() throws Exception {
        List<SysUser> users = List.of(
                new SysUser().setId(7L).setUserName("admin").setNickName("管理员"),
                new SysUser().setId(8L).setUserName("operator").setNickName("操作员"));
        AtomicInteger userQueries = new AtomicInteger();
        SysUserMapper userMapper = batchMapper(SysUserMapper.class, SysUser.class, users, userQueries);

        assertEquals(List.of("admin", "operator", "admin", "未知"),
                new UserNameTranslator(userMapper).translateBatch(
                        List.of(7L, "8", 7L, "invalid"), trans("userName")));
        assertEquals(1, userQueries.get());
        assertEquals(List.of("管理员", "操作员", "管理员", "操作员,管理员", "管理员,操作员"),
                new NicknameTranslator(userMapper).translateBatch(
                        List.of(7L, 8L, 7L, "8,7", "7,404,8"), trans("nickname")));
        assertEquals(2, userQueries.get());
        assertEquals(List.of("operator,admin", "admin,operator"),
                new UserNameTranslator(userMapper).translateBatch(
                        List.of("8,7", "7,404,8"), trans("userName")));
        assertEquals(3, userQueries.get());

        AtomicInteger deptQueries = new AtomicInteger();
        SysDeptMapper deptMapper = batchMapper(SysDeptMapper.class, SysDept.class, List.of(
                new SysDept().setId(9L).setDeptName("研发部"),
                new SysDept().setId(10L).setDeptName("运维部")), deptQueries);
        assertEquals(List.of("研发部", "运维部", "研发部", "运维部,研发部", "研发部,运维部"),
                new DeptNameTranslator(deptMapper).translateBatch(
                        List.of(9L, 10L, 9L, "10,9", "9,404,10"), trans("deptName")));
        assertEquals(1, deptQueries.get());
        assertEquals("管理员,操作员",
                new NicknameTranslator(userMapper).translate("7,8", trans("nickname")));
        assertEquals("研发部,运维部",
                new DeptNameTranslator(deptMapper).translate("9,10", trans("deptName")));
        assertEquals(4, userQueries.get());
        assertEquals(2, deptQueries.get());

        AtomicInteger ossQueries = new AtomicInteger();
        SysFileService fileService = (SysFileService) Proxy.newProxyInstance(
                SysFileService.class.getClassLoader(), new Class<?>[]{SysFileService.class},
                (proxy, method, args) -> {
                    if ("queryOssByIds".equals(method.getName())) {
                        ossQueries.incrementAndGet();
                        assertEquals(List.of("11", "12", "missing"), List.copyOf((Collection<?>) args[0]));
                        return List.of(
                                new SysOssVo().setOssId("11").setUrl("/file/avatar.png"),
                                new SysOssVo().setOssId("12").setUrl("/file/logo.png"));
                    }
                    return null;
                });
        assertEquals(List.of("/file/avatar.png,/file/logo.png", "/file/logo.png", "未知"),
                new OssUrlTranslator(fileService).translateBatch(
                        List.of("11,12", "12", "missing"), trans("ossUrl")));
        assertEquals(1, ossQueries.get());
    }

    @Test
    void upstreamVoFieldsDeclareSystemTranslations() throws Exception {
        assertTranslation(SysUserVo.class, "avatarUrl", TransType.OSS_URL, "avatar");
        assertTranslation(SysUserVo.class, "deptName", TransType.DEPT_NAME, "deptId");
        assertTranslation(ProfileUserVo.class, "avatarUrl", TransType.OSS_URL, "avatar");
        assertTranslation(ProfileUserVo.class, "deptName", TransType.DEPT_NAME, "deptId");
        assertTranslation(SysNoticeVo.class, "createByName", TransType.USER_NAME, "createBy");
        assertTranslation(SysOssVo.class, "createByName", TransType.USER_NAME, "createBy");
        assertTranslation(SysPostVo.class, "deptName", TransType.DEPT_NAME, "deptId");
    }

    private void assertTranslation(Class<?> type, String fieldName, TransType transType, String sourceField)
            throws Exception {
        Trans trans = type.getDeclaredField(fieldName).getAnnotation(Trans.class);
        assertEquals(transType, trans.type());
        assertEquals(sourceField, trans.field());
    }

    private Trans trans(String fieldName) throws Exception {
        return TranslationFixture.class.getDeclaredField(fieldName).getAnnotation(Trans.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T mapper(Class<T> mapperType, Class<?> entityType, Object entity) {
        return (T) Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[]{mapperType},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return entityType;
                    }
                    if ("getById".equals(method.getName()) && args != null && args.length == 1) {
                        Long id = ((Number) args[0]).longValue();
                        Long entityId = entity instanceof SysUser user ? user.getId() : ((SysDept) entity).getId();
                        return id.equals(entityId) ? entity : null;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private <T> T batchMapper(Class<T> mapperType, Class<?> entityType, List<?> entities,
                              AtomicInteger queryCount) {
        return (T) Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[]{mapperType},
                (proxy, method, args) -> {
                    if ("getEntityType".equals(method.getName())) {
                        return entityType;
                    }
                    if ("getMapperType".equals(method.getName())) {
                        return mapperType;
                    }
                    if ("list".equals(method.getName()) || "getById".equals(method.getName())) {
                        queryCount.incrementAndGet();
                        return "list".equals(method.getName()) ? entities : null;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    return null;
                });
    }

    private static final class TranslationFixture {
        @Trans(type = TransType.USER_NAME, defaultValue = "未知")
        private String userName;
        @Trans(type = TransType.NICKNAME, defaultValue = "未知")
        private String nickname;
        @Trans(type = TransType.DEPT_NAME, defaultValue = "未知")
        private String deptName;
        @Trans(type = TransType.OSS_URL, defaultValue = "未知")
        private String ossUrl;
    }
}
