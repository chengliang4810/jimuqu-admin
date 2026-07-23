package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysScheduledJobLog;
import com.jimuqu.system.domain.vo.ScheduledJobLogVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysScheduledJobLogMapper
        extends BaseMapperPlus<SysScheduledJobLog, ScheduledJobLogVo> {
}
