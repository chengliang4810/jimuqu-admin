package com.jimuqu.system.domain;

import cn.hutool.core.util.StrUtil;
import cn.hutool.v7.core.map.Dict;
import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.core.utils.JsonUtil;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;

import java.io.Serial;
import java.util.*;

/**
 * API密钥管理
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_api_key")
public class SysApiKey extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API密钥ID
     */
    @TableId(value = IdAutoType.GENERATOR, generatorName = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "API密钥ID")
    private Long id;

    /**
     * 绑定的用户ID
     */
    @AutoColumn(comment = "绑定的用户ID")
    private Long userId;

    /**
     * API Key值
     */
    @AutoColumn(comment = "API Key值", length = 100)
    private String apiKey;

    /**
     * 名称
     */
    @AutoColumn(comment = "名称", length = 50)
    private String name;

    /**
     * 备注
     */
    @AutoColumn(comment = "备注", length = 500)
    private String remark;

    /**
     * 权限范围（JSON格式存储）
     */
    @AutoColumn(comment = "权限范围（JSON格式存储）", length = 1000)
    private String scope;

    /**
     * 失效时间，13位时间戳，-1=永不失效
     */
    @AutoColumn(comment = "失效时间，13位时间戳，-1=永不失效")
    private Long expiresTime;

    /**
     * 是否有效
     */
    @AutoColumn(comment = "是否有效", defaultValue = "true")
    private Boolean isValid;

    /**
     * 扩展信息（JSON格式存储）
     */
    @AutoColumn(comment = "扩展信息（JSON格式存储）", length = 2000)
    private String extraData;

    /**
     * 命名空间
     */
    @AutoColumn(comment = "命名空间", length = 100, defaultValue = "default")
    private String namespace;

    /**
     * 临时存储权限范围的Set
     */
    @Ignore
    private transient Set<String> scopeSet = new HashSet<>();

    /**
     * 临时存储扩展信息的Map
     */
    @Ignore
    private transient Map<String, Object> extraMap = new HashMap<>();

    /**
     * 添加权限范围
     */
    public void addScope(String... scopes) {
        if (scopes != null) {
            scopeSet.addAll(Set.of(scopes));
        }
        updateScopeString();
    }

    /**
     * 移除权限范围
     */
    public void removeScope(String... scopes) {
        if (scopes != null) {
            for (String scope : scopes) {
                scopeSet.remove(scope);
            }
        }
        updateScopeString();
    }

    /**
     * 添加扩展信息
     */
    public void addExtra(String key, Object value) {
        extraMap.put(key, value);
        updateExtraString();
    }

    /**
     * 移除扩展信息
     */
    public void removeExtra(String key) {
        extraMap.remove(key);
        updateExtraString();
    }

    /**
     * 更新权限范围字符串
     */
    private void updateScopeString() {
        if (scopeSet.isEmpty()) {
            this.scope = null;
        } else {
            this.scope = String.join(",", scopeSet);
        }
    }

    /**
     * 更新扩展信息字符串
     */
    private void updateExtraString() {
        if (extraMap.isEmpty()) {
            this.extraData = null;
        } else {
            this.extraData = JsonUtil.toJsonString(extraMap);
        }
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        if (expiresTime == null || expiresTime == -1) {
            return false;
        }
        return System.currentTimeMillis() > expiresTime;
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasScope(String scope) {
        return scopeSet.contains(scope);
    }

    /**
     * 获取扩展信息
     */
    public Object getExtraValue(String key) {
        return extraMap.get(key);
    }

    /**
     * 解析扩展信息字符串到Map
     */
    private void parseExtraString() {
        if (StrUtil.isNotBlank(extraData)) {
            try {
                Dict dict = JsonUtil.parseMap(extraData);
                extraMap.clear();
                for (String key : dict.keySet()) {
                    extraMap.put(key, dict.get(key));
                }
            } catch (Exception e) {
                // JSON解析失败，清空扩展信息
                extraMap.clear();
            }
        }
    }

    /**
     * 获取解析后的扩展信息Map
     */
    public Map<String, Object> getExtraMap() {
        if (extraMap.isEmpty() && StrUtil.isNotBlank(extraData)) {
            parseExtraString();
        }
        return extraMap;
    }
}