package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.Where;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.system.domain.SysPost;
import com.jimuqu.system.domain.SysUserPost;
import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.domain.query.SysPostQuery;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * 岗位信息Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysPostServiceImpl implements SysPostService {

    private final SysDeptMapper sysDeptMapper;
    private final SysPostMapper sysPostMapper;

    /**
     * 查询岗位信息
     */
    @Override
    public SysPostVo queryById(Long id) {
        return sysPostMapper.getVoById(id);
    }

    /**
     * 查询岗位信息分页列表
     */
    @Override
    public Page<SysPostVo> queryPageList(SysPostQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysPostVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询岗位信息列表
     */
    @Override
    public List<SysPostVo> queryList(SysPostQuery query) {
        QueryChain<SysPost> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysPostVo.class).list();
    }

    /**
     * 构建查询条件
     *
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysPost> buildQueryChain(SysPostQuery query) {
        QueryChain<SysPost> queryChain = QueryChain.of(sysPostMapper)
                .forSearch(true)
                .where(query);

        if (ObjUtil.isNotNull(query.getBelongDeptId())){
            List<Long> deptIds = sysDeptMapper.selectListByParentId(query.getBelongDeptId());
            deptIds.add(query.getBelongDeptId());
            queryChain.in(SysPost::getDeptId, deptIds);
        }
        return queryChain;
    }

    /**
     * 新增岗位信息
     */
    @Override
    public Boolean insertByBo(SysPostBo bo) {
        SysPost sysPost = MapstructUtil.convert(bo, SysPost.class);
        boolean flag = sysPostMapper.save(sysPost) > 0;
        bo.setPostId(sysPost.getPostId());
        return flag;
    }

    /**
     * 修改岗位信息
     */
    @Override
    public Boolean updateByBo(SysPostBo bo) {
        SysPost sysPost = MapstructUtil.convert(bo, SysPost.class);
        return sysPostMapper.update(sysPost) > 0;
    }

    /**
     * 批量删除岗位信息
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysPostMapper.deleteByIds(ids);
    }


    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    @Override
    public List<Long> selectPostListByUserId(Long userId) {
        List<SysPost> sysPostList = QueryChain.of(sysPostMapper)
                .select(SysPost::getPostId)
                .leftJoin(SysPost::getPostId, SysUserPost::getPostId)
                .eq(SysUserPost::getUserId, userId)
                .list();
        return Optional.ofNullable(sysPostList)
                .map(sysPosts -> sysPosts.stream().map(SysPost::getPostId).toList())
                .orElse(ListUtil.zero());
    }

    /**
     * 校验岗位名称
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostNameUnique(SysPostBo post) {
        boolean exists = sysPostMapper.exists(Where.create(SysPost.class)
                .eq(SysPost::getPostName, post.getPostName())
                .ne(ObjUtil.isNotNull(post.getPostId()), SysPost::getPostId, post.getPostId()));
        return !exists;
    }

    /**
     * 校验岗位编码
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostCodeUnique(SysPostBo post) {
        boolean exists = sysPostMapper.exists(Where.create(SysPost.class)
                .eq(SysPost::getPostCode, post.getPostCode())
                .ne(ObjUtil.isNotNull(post.getPostId()), SysPost::getPostId, post.getPostId()));
        return !exists;
    }

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int countUserPostById(Long postId) {
        return sysPostMapper.count(Where.create(SysUserPost.class)
                .eq(SysUserPost::getPostId, postId));
    }
}
