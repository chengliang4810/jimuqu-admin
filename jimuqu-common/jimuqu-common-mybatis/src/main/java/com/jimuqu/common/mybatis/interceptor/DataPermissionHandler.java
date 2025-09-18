package com.jimuqu.common.mybatis.interceptor;

import cn.hutool.v7.core.text.StrUtil;
import com.jimuqu.common.mybatis.annotation.DataColumn;
import com.jimuqu.common.mybatis.annotation.DataPermission;
import com.jimuqu.common.mybatis.enums.DataScopeType;
import com.jimuqu.common.mybatis.expression.SolonBeanResolver;
import com.jimuqu.common.mybatis.expression.SolonExpressionParser;
import com.jimuqu.common.mybatis.helper.DataPermissionHelper;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.common.core.domain.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 数据权限处理器
 * <p>
 * 负责根据用户角色和权限类型生成SQL过滤条件
 * 支持SpEL表达式解析和动态变量替换
 *
 * @author chengliang4810
 * @version 1.0
 */
@Slf4j
public class DataPermissionHandler {

    /**
     * 系统数据权限服务Bean名称
     */
    private static final String SYS_DATA_SCOPE_SERVICE = "sysDataScopeService";

    /**
     * Solon Bean解析器
     */
    private static final SolonBeanResolver BEAN_RESOLVER = new SolonBeanResolver();

    /**
     * 处理数据权限，生成SQL条件
     *
     * @param dataPermission 数据权限注解
     * @param alias          表别名
     * @return 生成的SQL条件
     */
    public String handle(DataPermission dataPermission, String alias) {
        try {
            LoginUser user = LoginHelper.getLoginUser();
            if (user == null) {
                return "";
            }

            // 获取用户的数据权限类型
            String dataScope = user.getDataScope();
            if (dataScope == null || dataScope.isEmpty()) {
                return "";
            }

            // 构建表达式解析上下文
            Map<String, Object> context = buildEvaluationContext(user, dataPermission, alias);

            // 生成SQL条件
            return buildSqlCondition(dataScope, context);

        } catch (Exception e) {
            log.error("处理数据权限时发生错误", e);
            return "";
        }
    }

    /**
     * 构建表达式解析上下文
     */
    private Map<String, Object> buildEvaluationContext(LoginUser user, DataPermission dataPermission, String alias) {
        Map<String, Object> context = new HashMap<>();

        // 设置当前用户
        context.put("user", user);

        // 设置数据权限映射关系
        for (DataColumn dataColumn : dataPermission.value()) {
            String key = dataColumn.key();
            String value = dataColumn.value();
            if (alias != null && !alias.isEmpty()) {
                value = alias + "." + value;
            }
            context.put(key, value);
        }

        // 设置系统数据权限服务
        try {
            Object dataScopeService = Solon.context().getBean(SYS_DATA_SCOPE_SERVICE);
            context.put("sdss", dataScopeService);
        } catch (Exception e) {
            log.warn("未找到系统数据权限服务: {}", SYS_DATA_SCOPE_SERVICE, e);
        }

        // 设置额外的上下文变量
        Map<String, Object> extraVariables = DataPermissionHelper.getContext();
        context.putAll(extraVariables);

        return context;
    }

    /**
     * 构建SQL条件
     */
    private String buildSqlCondition(String dataScope, Map<String, Object> context) {
        // 支持多角色权限，用逗号分隔
        String[] scopes = dataScope.split(",");
        StringJoiner conditions = new StringJoiner(" OR ");

        for (String scope : scopes) {
            DataScopeType scopeType = DataScopeType.findCode(scope.trim());
            if (scopeType != null) {
                String condition = buildSingleCondition(scopeType, context);
                if (condition != null && !condition.trim().isEmpty()) {
                    conditions.add("(" + condition + ")");
                }
            }
        }

        return conditions.length() > 0 ? conditions.toString() : "";
    }

    /**
     * 构建单个权限条件
     */
    private String buildSingleCondition(DataScopeType scopeType, Map<String, Object> context) {
        try {
            String sqlTemplate = scopeType.getSqlTemplate();
            if (sqlTemplate == null || sqlTemplate.trim().isEmpty()) {
                return "";
            }

            // 使用Solon表达式解析器解析模板
            String result = SolonExpressionParser.parse(sqlTemplate, context, BEAN_RESOLVER);

            if (StrUtil.isNotBlank(result)) {
                // 移除前后的 #{ } 和 空格
                result = result.replaceAll("^#\\{\\s*", "")
                        .replaceAll("\\s*\\}$", "")
                        .trim();
                return result;
            }

            return "";

        } catch (Exception e) {
            log.error("解析数据权限条件时发生错误: {}", scopeType.getCode(), e);
            return scopeType.getElseSql();
        }
    }

    /**
     * 检查用户是否有数据权限
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是否有权限
     */
    public boolean checkDataPermission(Long userId, Long deptId) {
        try {
            LoginUser user = LoginHelper.getLoginUser();
            if (user == null) {
                return false;
            }

            // 超级管理员拥有所有权限
            if (LoginHelper.isSuperAdmin()) {
                return true;
            }

            // 获取用户的数据权限类型
            String dataScope = user.getDataScope();
            if (dataScope == null || dataScope.isEmpty()) {
                return false;
            }

            // 检查各种权限类型
            return Arrays.stream(dataScope.split(","))
                    .map(String::trim)
                    .map(DataScopeType::findCode)
                            .filter(java.util.Objects::nonNull)
                            .anyMatch(scopeType -> checkSinglePermission(scopeType, userId, deptId));

        } catch (Exception e) {
            log.error("检查数据权限时发生错误", e);
            return false;
        }
    }

    /**
     * 检查单个权限类型
     */
    private boolean checkSinglePermission(DataScopeType scopeType, Long userId, Long deptId) {
        switch (scopeType) {
            case ALL:
                return true;
            case DEPT:
                return deptId.equals(LoginHelper.getDeptId());
            case DEPT_AND_CHILD:
                try {
                    Object dataScopeService = Solon.context().getBean(SYS_DATA_SCOPE_SERVICE);
                    if (dataScopeService instanceof ISysDataScopeService) {
                        ISysDataScopeService service = (ISysDataScopeService) dataScopeService;
                        return service.getDeptAndChild(LoginHelper.getDeptId()).contains(deptId);
                    }
                } catch (Exception e) {
                    log.error("获取部门权限时发生错误", e);
                }
                return false;
            case SELF:
                return userId.equals(LoginHelper.getUserId());
            case CUSTOM:
                try {
                    Object dataScopeService = Solon.context().getBean(SYS_DATA_SCOPE_SERVICE);
                    if (dataScopeService instanceof ISysDataScopeService) {
                        ISysDataScopeService service = (ISysDataScopeService) dataScopeService;
                        return service.getRoleCustom(LoginHelper.getLoginUser().getRoleId()).contains(deptId);
                    }
                } catch (Exception e) {
                    log.error("获取自定义权限时发生错误", e);
                }
                return false;
            default:
                return false;
        }
    }

}