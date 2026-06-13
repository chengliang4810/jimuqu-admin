package com.jimuqu.system.domain;

import cn.xbatis.core.incrementer.IdentifierGeneratorType;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import com.jimuqu.common.mybatis.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.dromara.autotable.annotation.AutoColumn;
import org.dromara.autotable.annotation.mysql.MysqlTypeConstant;

import java.io.Serial;
import java.util.Date;

/**
 * 定时任务运行日志
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Data
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_job_log")
public class SysJobLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @TableId(value = IdAutoType.GENERATOR, generator = IdentifierGeneratorType.DEFAULT)
    @AutoColumn(comment = "日志主键")
    private Long id;

    /**
     * 任务主键
     */
    @AutoColumn(comment = "任务主键")
    private Long jobId;

    /**
     * 任务名称
     */
    @AutoColumn(comment = "任务名称", length = 100)
    private String jobName;

    /**
     * 任务分组
     */
    @AutoColumn(comment = "任务分组", length = 100)
    private String jobGroup;

    /**
     * 白名单处理器标识
     */
    @AutoColumn(comment = "白名单处理器标识", length = 200)
    private String handlerKey;

    /**
     * 处理器参数JSON
     */
    @AutoColumn(comment = "处理器参数JSON", type = MysqlTypeConstant.TEXT)
    private String handlerParam;

    /**
     * 运行状态（0成功 1失败 2跳过）
     */
    @AutoColumn(comment = "运行状态（0成功 1失败 2跳过）")
    private Integer status;

    /**
     * 开始时间
     */
    @AutoColumn(comment = "开始时间", type = MysqlTypeConstant.DATETIME)
    private Date startTime;

    /**
     * 结束时间
     */
    @AutoColumn(comment = "结束时间", type = MysqlTypeConstant.DATETIME)
    private Date endTime;

    /**
     * 耗时毫秒
     */
    @AutoColumn(comment = "耗时毫秒")
    private Long durationMs;

    /**
     * 错误信息
     */
    @AutoColumn(comment = "错误信息", type = MysqlTypeConstant.TEXT)
    private String errorMessage;

    /**
     * 结果类型
     */
    @AutoColumn(comment = "结果类型", length = 50)
    private String resultType;

    /**
     * 结果文件名
     */
    @AutoColumn(comment = "结果文件名", length = 255)
    private String resultFileName;

    /**
     * 结果文件路径
     */
    @AutoColumn(comment = "结果文件路径", length = 1000)
    private String resultFilePath;

    /**
     * 结果内容类型
     */
    @AutoColumn(comment = "结果内容类型", length = 200)
    private String resultContentType;

    /**
     * 结果文件大小
     */
    @AutoColumn(comment = "结果文件大小")
    private Long resultFileSize;

    /**
     * 导出总行数
     */
    @AutoColumn(comment = "导出总行数")
    private Long resultTotalRows;

    /**
     * 导出文件数量
     */
    @AutoColumn(comment = "导出文件数量")
    private Integer resultFileCount;
}
