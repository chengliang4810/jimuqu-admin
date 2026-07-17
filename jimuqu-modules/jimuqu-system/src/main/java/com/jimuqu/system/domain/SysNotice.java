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
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

/**
 * 通知公告。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_notice")
public class SysNotice extends BaseEntity {

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "公告ID")
    private Long noticeId;

    @AutoColumn(comment = "公告标题", length = 50, notNull = true)
    private String noticeTitle;

    @AutoColumn(comment = "公告类型（1通知 2公告）", length = 1)
    private String noticeType;

    @AutoColumn(comment = "公告内容", type = MysqlTypeConstant.TEXT)
    private String noticeContent;

    @AutoColumn(comment = "公告状态（0正常 1关闭）", length = 1, defaultValue = "0")
    private String status;

    @AutoColumn(comment = "备注", length = 500)
    private String remark;
}
