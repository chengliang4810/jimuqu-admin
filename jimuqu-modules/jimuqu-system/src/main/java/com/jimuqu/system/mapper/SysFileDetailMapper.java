package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysFileDetail;
import com.jimuqu.system.domain.vo.SysFileDetailVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件记录数据层
 * @author chengliang4810
 * @since 2025-06-24
 */
@Mapper
public interface SysFileDetailMapper extends BaseMapperPlus<SysFileDetail, SysFileDetailVo> {

}