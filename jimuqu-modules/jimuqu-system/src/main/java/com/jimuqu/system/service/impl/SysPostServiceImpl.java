package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.Where;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysPost;
import com.jimuqu.system.domain.SysUserPost;
import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.domain.query.SysPostQuery;
import com.jimuqu.system.domain.vo.SysPostVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.mapper.SysUserPostMapper;
import com.jimuqu.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


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
    private final SysUserPostMapper sysUserPostMapper;
    private final ISysDataScopeService dataScopeService;

    /**
     * 查询岗位信息
     */
    @Override
    public SysPostVo queryById(Long id) {
        QueryChain<SysPost> queryChain = QueryChain.of(sysPostMapper)
                .eq(SysPost::getPostId, id);
        applyDataScope(queryChain);
        SysPostVo post = queryChain.returnType(SysPostVo.class).get();
        enrichDeptNames(post == null ? List.of() : List.of(post));
        return post;
    }

    /**
     * 查询岗位信息分页列表
     */
    @Override
    public Page<SysPostVo> queryPageList(SysPostQuery query, PageQuery pageQuery) {
        Page<SysPostVo> page = pageQuery.applyOrder(buildQueryChain(query))
                .returnType(SysPostVo.class)
                .paging(pageQuery.build());
        enrichDeptNames(page.getRows());
        return page;
    }

    /**
     * 查询岗位信息列表
     */
    @Override
    public List<SysPostVo> queryList(SysPostQuery query) {
        QueryChain<SysPost> queryChain = buildQueryChain(query);
        List<SysPostVo> posts = queryChain.returnType(SysPostVo.class).list();
        enrichDeptNames(posts);
        return posts;
    }

    private void enrichDeptNames(List<SysPostVo> posts) {
        Set<Long> deptIds = posts.stream().map(SysPostVo::getDeptId)
                .filter(ObjUtil::isNotNull).collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, SysDept> depts = QueryChain.of(sysDeptMapper)
                .select(SysDept::getId, SysDept::getDeptName)
                .in(SysDept::getId, deptIds)
                .list().stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
        posts.forEach(post -> {
            SysDept dept = depts.get(post.getDeptId());
            post.setDeptName(dept == null ? null : dept.getDeptName());
        });
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
                .where(query)
                .orderBy(SysPost::getPostSort, SysPost::getPostId);

        applyDepartmentFilter(queryChain, query);
        applyDataScope(queryChain);
        return queryChain;
    }

    void applyDepartmentFilter(QueryChain<SysPost> queryChain, SysPostQuery query) {
        if (ObjUtil.isNull(query.getDeptId()) && ObjUtil.isNotNull(query.getBelongDeptId())) {
            List<Long> deptIds = sysDeptMapper.selectListByParentId(query.getBelongDeptId());
            deptIds.add(query.getBelongDeptId());
            queryChain.in(SysPost::getDeptId, deptIds);
        }
    }

    private void applyDataScope(QueryChain<SysPost> queryChain) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (rule.allAccess()) {
            return;
        }
        boolean hasDepartments = !rule.departmentIds().isEmpty();
        boolean hasSelf = rule.selfAccess() && rule.userId() != null;
        if (hasDepartments && hasSelf) {
            queryChain.andNested(scope -> scope.in(SysPost::getDeptId, rule.departmentIds())
                    .or().eq(SysPost::getCreateBy, rule.userId()));
        } else if (hasDepartments) {
            queryChain.in(SysPost::getDeptId, rule.departmentIds());
        } else if (hasSelf) {
            queryChain.eq(SysPost::getCreateBy, rule.userId());
        } else {
            queryChain.eq(SysPost::getPostId, -1L);
        }
    }

    /**
     * 新增岗位信息
     */
    @Override
    public Boolean insertByBo(SysPostBo bo) {
        assertDepartmentExists(bo.getDeptId());
        assertWriteScope(LoginHelper.getUserId(), bo.getDeptId());
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
        SysPost current = getAccessiblePosts(List.of(bo.getPostId())).stream().findFirst()
                .orElseThrow(() -> new ServiceException("岗位不存在或无权访问"));
        assertDepartmentExists(bo.getDeptId());
        assertWriteScope(current.getCreateBy(), bo.getDeptId());
        SysPost sysPost = MapstructUtil.convert(bo, SysPost.class);
        return sysPostMapper.update(sysPost) > 0;
    }

    /**
     * 批量删除岗位信息
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        Set<Long> requested = new HashSet<>(ids);
        List<SysPost> posts = getAccessiblePosts(requested);
        if (posts.size() != requested.size()) {
            throw new ServiceException("岗位不存在或无权访问");
        }
        for (SysPost post : posts) {
            if (countUserPostById(post.getPostId()) > 0) {
                throw new ServiceException(post.getPostName() + "已分配，不能删除!");
            }
        }
        return sysPostMapper.deleteByIds(requested);
    }

    private List<SysPost> getAccessiblePosts(Collection<Long> ids) {
        QueryChain<SysPost> queryChain = QueryChain.of(sysPostMapper)
                .in(SysPost::getPostId, ids);
        applyDataScope(queryChain);
        return queryChain.list();
    }

    private void assertDepartmentExists(Long deptId) {
        if (deptId == null || sysDeptMapper.getById(deptId) == null) {
            throw new ServiceException("部门不存在");
        }
    }

    private void assertWriteScope(Long recordUserId, Long deptId) {
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
        if (!rule.permits(recordUserId, deptId)) {
            throw new ServiceException("没有权限访问岗位数据");
        }
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

    @Override
    public List<SysPostVo> selectPostsByUserId(Long userId) {
        return QueryChain.of(sysPostMapper)
                .select(SysPost.class)
                .leftJoin(SysPost::getPostId, SysUserPost::getPostId)
                .eq(SysUserPost::getUserId, userId)
                .orderBy(SysPost::getPostSort, SysPost::getPostId)
                .returnType(SysPostVo.class)
                .list();
    }

    @Override
    public List<SysPostVo> selectPostByIds(Collection<Long> postIds) {
        QueryChain<SysPost> queryChain = QueryChain.of(sysPostMapper)
                .select(SysPost::getPostId, SysPost::getPostName, SysPost::getPostCode)
                .eq(SysPost::getStatus, "0")
                .in(postIds != null && !postIds.isEmpty(), SysPost::getPostId, postIds)
                .orderBy(SysPost::getPostSort, SysPost::getPostId);
        applyDataScope(queryChain);
        return queryChain
                .returnType(SysPostVo.class)
                .list();
    }

    /**
     * 校验岗位名称
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public boolean checkPostNameUnique(SysPostBo post) {
        boolean exists = sysPostMapper.exists(Where.create()
                .eq(SysPost::getPostName, post.getPostName())
                .eq(SysPost::getDeptId, post.getDeptId())
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
        boolean exists = sysPostMapper.exists(Where.create()
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
        return Math.toIntExact(QueryChain.of(sysUserPostMapper)
                .eq(SysUserPost::getPostId, postId)
                .count());
    }

    @Override
    public long countPostByDeptId(Long deptId) {
        return QueryChain.of(sysPostMapper).eq(SysPost::getDeptId, deptId).count();
    }
}
