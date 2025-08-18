package com.jimuqu.system.domain.bo;

import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.mybatis.core.entity.BoBaseEntity;
import com.jimuqu.system.domain.SysApiKey;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.noear.solon.validation.annotation.*;

/**
 * API密钥业务对象
 * @author jimuqu-admin
 * @since 2025-08-18
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysApiKey.class, reverseConvertGenerate = false)
public class SysApiKeyBo extends BoBaseEntity {

    /**
     * API密钥ID
     */
    @NotNull(message = "API密钥ID不能为空", groups = { UpdateGroup.class })
    private Long id;

    /**
     * 绑定的用户ID
     */
    @NotNull(message = "绑定的用户ID不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Long userId;

    /**
     * API Key值
     */
    @NotBlank(message = "API Key值不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String apiKey;

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 权限范围（JSON格式存储）
     */
    private String scope;

    /**
     * 失效时间，13位时间戳，-1=永不失效
     */
    private Long expiresTime;

    /**
     * 是否有效
     */
    @NotNull(message = "是否有效不能为空", groups = { AddGroup.class, UpdateGroup.class })
    private Boolean isValid;

    /**
     * 扩展信息（JSON格式存储）
     */
    private String extraData;

    /**
     * 命名空间
     */
    private String namespace;
}