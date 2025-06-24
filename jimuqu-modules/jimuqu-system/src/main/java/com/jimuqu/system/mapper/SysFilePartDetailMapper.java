package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysFilePartDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分片信息，仅在手动分片上传时使用数据层
 * @author chengliang4810
 * @since 2025-06-24
 */
@Mapper
public interface SysFilePartDetailMapper extends BaseMapperPlus<SysFilePartDetail, SysFilePartDetail> {

}