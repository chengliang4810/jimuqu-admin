package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysNotice;
import com.jimuqu.system.domain.vo.SysNoticeVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysNoticeMapper extends BaseMapperPlus<SysNotice, SysNoticeVo> {
}
