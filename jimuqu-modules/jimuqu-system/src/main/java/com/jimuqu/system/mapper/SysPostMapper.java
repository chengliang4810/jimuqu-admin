package com.jimuqu.system.mapper;


import com.jimuqu.common.mybatis.core.mapper.BaseMapperPlus;
import com.jimuqu.system.domain.SysPost;
import com.jimuqu.system.domain.vo.SysPostVo;

import java.util.List;

/**
 * 岗位信息 数据层
 *
 * @author Lion Li,chengliang4810
 */
public interface SysPostMapper extends BaseMapperPlus<SysPost, SysPostVo> {

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    List<Long> selectPostListByUserId(Long userId);

    /**
     * 查询用户所属岗位组
     *
     * @param userName 用户名
     * @return 结果
     */
    List<SysPostVo> selectPostsByUserName(String userName);

}
