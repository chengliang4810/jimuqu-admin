package com.jimuqu.system.mapper;

import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysPlugin;
import com.jimuqu.system.domain.vo.SysPluginVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 在线插件数据层。
 *
 * @author jimuqu-admin
 * @since 2026-06-13
 */
@Mapper
public interface SysPluginMapper extends BaseMapperPlus<SysPlugin, SysPluginVo> {
}
