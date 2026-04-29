package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysJobLog;
import com.jimuqu.system.domain.vo.SysJobLogVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务运行日志数据层
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Mapper
public interface SysJobLogMapper extends BaseMapperPlus<SysJobLog, SysJobLogVo> {
}
