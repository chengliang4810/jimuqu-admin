package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysMessage;
import com.jimuqu.system.domain.vo.SysMessageVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysMessageMapper extends BaseMapperPlus<SysMessage, SysMessageVo> {
}
