package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysSocial;
import com.jimuqu.system.domain.vo.SysSocialVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社会化账号绑定数据层。
 */
@Mapper
public interface SysSocialMapper extends BaseMapperPlus<SysSocial, SysSocialVo> {
}
