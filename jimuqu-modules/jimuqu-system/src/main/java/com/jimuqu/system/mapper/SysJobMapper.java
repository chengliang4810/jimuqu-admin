package com.jimuqu.system.mapper;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysJob;
import com.jimuqu.system.domain.vo.SysJobVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 定时任务数据层
 *
 * @author jimuqu-admin
 * @since 2026-04-29
 */
@Mapper
public interface SysJobMapper extends BaseMapperPlus<SysJob, SysJobVo> {

    /**
     * 查询启用中的任务
     *
     * @return 任务列表
     */
    default List<SysJob> listEnabled() {
        return QueryChain.of(this)
                .where(where -> where.eq(SysJob::getStatus, 0))
                .list();
    }
}
