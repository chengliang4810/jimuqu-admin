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
import org.dromara.autotable.annotation.TableIndex;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Table("sys_message")
@TableIndex(name = "sys_message_category_time", fields = {"category", "createTime"})
public class SysMessage extends BaseEntity {

    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "消息ID")
    private Long messageId;
    @AutoColumn(comment = "消息分类", length = 32, notNull = true)
    private String category;
    @AutoColumn(comment = "消息类型", length = 32, notNull = true)
    private String type;
    @AutoColumn(comment = "消息来源", length = 32, notNull = true)
    private String source;
    @AutoColumn(comment = "消息标题", length = 200)
    private String title;
    @AutoColumn(comment = "消息摘要", length = 500)
    private String message;
    @AutoColumn(comment = "消息内容", type = MysqlTypeConstant.TEXT)
    private String content;
    @AutoColumn(comment = "消息数据", type = MysqlTypeConstant.TEXT)
    private String dataJson;
    @AutoColumn(comment = "跳转路径", length = 500)
    private String path;
    @AutoColumn(comment = "接收用户ID", length = 2000, notNull = true)
    private String sendUserIds;
}
