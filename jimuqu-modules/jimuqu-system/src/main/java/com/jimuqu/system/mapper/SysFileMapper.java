package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysFile;
import com.jimuqu.system.domain.vo.SysFileVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件记录数据层
 * @author chengliang4810
 * @since 2025-06-24
 */
@Mapper
public interface SysFileMapper extends BaseMapperPlus<SysFile, SysFileVo> {

}