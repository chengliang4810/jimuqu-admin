package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysLoginInfo;
import com.jimuqu.system.domain.vo.SysLoginInfoVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginInfoMapper extends BaseMapperPlus<SysLoginInfo, SysLoginInfoVo> {
}
