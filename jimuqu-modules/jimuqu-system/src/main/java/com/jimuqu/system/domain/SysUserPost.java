package com.jimuqu.system.domain;

import cn.xbatis.db.annotations.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.PrimaryKey;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author chengliang
 * @date 2024/06/13
 */
@Data
@FieldNameConstants
@Table("sys_user_post")
public class SysUserPost {

    /**
     * 用户ID
     */
    @PrimaryKey
    @AutoColumn(comment = "用户ID", notNull = true)
    private Long userId;

    /**
     * 岗位ID
     */
    @PrimaryKey
    @AutoColumn(comment = "岗位ID", notNull = true)
    private Long postId;

}
