package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.Index;
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.enums.IndexTypeEnum;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

/**
 * 社会化账号绑定关系。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_social")
@TableIndex(name = "uk_sys_social_user_source", type = IndexTypeEnum.UNIQUE,
        fields = {"userId", "source"})
public class SysSocial extends BaseEntity {

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "主键")
    private Long id;

    @AutoColumn(comment = "用户ID", notNull = true)
    private Long userId;

    @AutoColumn(comment = "平台和平台唯一ID", length = 255, notNull = true)
    @Index(name = "uk_sys_social_auth_id", type = IndexTypeEnum.UNIQUE)
    private String authId;

    @AutoColumn(comment = "用户来源", length = 64, notNull = true)
    private String source;

    @AutoColumn(comment = "授权令牌", type = MysqlTypeConstant.TEXT)
    private String accessToken;

    @AutoColumn(comment = "授权令牌有效期")
    private int expireIn;

    @AutoColumn(comment = "刷新令牌", type = MysqlTypeConstant.TEXT)
    private String refreshToken;

    @AutoColumn(comment = "平台唯一ID", length = 255)
    private String openId;

    @AutoColumn(comment = "第三方账号", length = 255)
    private String userName;

    @AutoColumn(comment = "第三方昵称", length = 255)
    private String nickName;

    @AutoColumn(comment = "第三方邮箱", length = 255)
    private String email;

    @AutoColumn(comment = "第三方头像", type = MysqlTypeConstant.TEXT)
    private String avatar;

    @AutoColumn(comment = "平台授权信息", type = MysqlTypeConstant.TEXT)
    private String accessCode;

    @AutoColumn(comment = "平台 unionId", length = 255)
    private String unionId;

    @AutoColumn(comment = "授权范围", type = MysqlTypeConstant.TEXT)
    private String scope;

    @AutoColumn(comment = "令牌类型", length = 64)
    private String tokenType;

    @AutoColumn(comment = "ID令牌", type = MysqlTypeConstant.TEXT)
    private String idToken;

    @AutoColumn(comment = "MAC算法", length = 255)
    private String macAlgorithm;

    @AutoColumn(comment = "MAC密钥", type = MysqlTypeConstant.TEXT)
    private String macKey;

    @AutoColumn(comment = "授权码", type = MysqlTypeConstant.TEXT)
    private String code;

    @AutoColumn(comment = "OAuth令牌", type = MysqlTypeConstant.TEXT)
    private String oauthToken;

    @AutoColumn(comment = "OAuth令牌密钥", type = MysqlTypeConstant.TEXT)
    private String oauthTokenSecret;
}
