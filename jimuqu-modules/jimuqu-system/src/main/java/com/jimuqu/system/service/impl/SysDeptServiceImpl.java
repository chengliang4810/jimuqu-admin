package com.jimuqu.system.service.impl;

import cn.xbatis.core.sql.executor.chain.QueryChain;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.core.utils.MapstructUtil;
import com.jimuqu.common.core.utils.StreamUtil;
import com.jimuqu.common.core.utils.TreeBuildUtil;
import com.jimuqu.common.mybatis.core.Page;
import com.jimuqu.common.mybatis.core.page.PageQuery;
import com.jimuqu.common.mybatis.model.DataScopeRule;
import com.jimuqu.common.mybatis.service.ISysDataScopeService;
import com.jimuqu.common.satoken.utils.LoginHelper;
import com.jimuqu.system.domain.SysDept;
import com.jimuqu.system.domain.SysRole;
import com.jimuqu.system.domain.SysUser;
import com.jimuqu.system.domain.bo.SysDeptBo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysRoleMapper;
import com.jimuqu.system.mapper.SysUserMapper;
import com.jimuqu.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.tree.MapTree;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.annotation.Component;
import org.noear.solon.data.annotation.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * 部门Service业务层处理
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final ISysDataScopeService dataScopeService;

    /**
     * 查询部门
     */
    @Override
    public SysDeptVo queryById(Long id) {
        return sysDeptMapper.getVoById(id);
    }

    /**
     * 查询部门分页列表
     */
    @Override
    public Page<SysDeptVo> queryPageList(SysDeptQuery query, PageQuery pageQuery) {
        return buildQueryChain(query)
                .returnType(SysDeptVo.class)
                .paging(pageQuery.build());
    }

    /**
     * 查询部门列表
     */
    @Override
    public List<SysDeptVo> queryList(SysDeptQuery query) {
        QueryChain<SysDept> queryChain = buildQueryChain(query);
        return queryChain.returnType(SysDeptVo.class).list();
    }

    @Override
    public List<SysDeptVo> selectByIds(Collection<Long> ids) {
        return QueryChain.of(sysDeptMapper)
                .eq(SysDept::getStatus, "0")
                .in(CollUtil.isNotEmpty(ids), SysDept::getId, ids)
                .orderBy(SysDept::getOrderNum, SysDept::getId)
                .returnType(SysDeptVo.class).list();
    }

    /**
     * 构建查询条件
     * @param query 查询对象
     * @return 查询条件对象
     */
    private QueryChain<SysDept> buildQueryChain(SysDeptQuery query) {
        QueryChain<SysDept> sysDeptQueryChain = QueryChain.of(sysDeptMapper)
                .forSearch(true)
                .where(query)
                .orderBy(SysDept::getAncestors, SysDept::getParentId, SysDept::getOrderNum, SysDept::getId);

        Long belongDeptId = query.getBelongDeptId();
        if (ObjUtil.isNotNull(belongDeptId)) {
            List<Long> deptList = sysDeptMapper.selectListByParentId(belongDeptId);
            deptList.add(belongDeptId);
            if (CollUtil.isNotEmpty(deptList)) {
                sysDeptQueryChain.in(SysDept::getId, deptList);
            }
        }

        if (!LoginHelper.isSuperAdmin()) {
            DataScopeRule rule = dataScopeService.resolveUserDataScope(LoginHelper.getUserId());
            if (!rule.allAccess()) {
                if (rule.departmentIds().isEmpty()) {
                    sysDeptQueryChain.eq(SysDept::getId, -1L);
                } else {
                    sysDeptQueryChain.in(SysDept::getId, rule.departmentIds());
                }
            }
        }

        return sysDeptQueryChain;
    }

    /**
     * 新增部门
     */
    @Override
    public Boolean insertByBo(SysDeptBo bo) {
        SysDept parent = sysDeptMapper.getById(bo.getParentId());
        if (parent == null) {
            throw new ServiceException("父部门不存在");
        }
        if (!"0".equals(parent.getStatus())) {
            throw new ServiceException("部门停用，不允许新增");
        }
        SysDept sysDept = MapstructUtil.convert(bo, SysDept.class);
        sysDept.setAncestors(parent.getAncestors() + "," + parent.getId());
        boolean flag = sysDeptMapper.save(sysDept) > 0;
        bo.setId(sysDept.getId());
        return flag;
    }

    /**
     * 修改部门
     */
    @Override
    @Transaction
    public Boolean updateByBo(SysDeptBo bo) {
        SysDept sysDept = MapstructUtil.convert(bo, SysDept.class);
        SysDept old = sysDeptMapper.getById(bo.getId());
        if (old == null) {
            throw new ServiceException("部门不存在，无法修改");
        }
        if (!Objects.equals(old.getParentId(), bo.getParentId())) {
            checkDeptDataScope(bo.getParentId());
            SysDept parent = sysDeptMapper.getById(bo.getParentId());
            if (parent == null) {
                throw new ServiceException("父部门不存在");
            }
            String newAncestors = parent.getAncestors() + "," + parent.getId();
            updateChildrenAncestors(bo.getId(), newAncestors, old.getAncestors());
            sysDept.setAncestors(newAncestors);
        } else {
            sysDept.setAncestors(old.getAncestors());
        }
        int rows = sysDeptMapper.update(sysDept);
        if ("0".equals(sysDept.getStatus()) && com.jimuqu.common.core.utils.StringUtil.isNotBlank(sysDept.getAncestors())) {
            List<Long> parentIds = com.jimuqu.common.core.utils.StringUtil.splitTo(
                    sysDept.getAncestors(), value -> Long.valueOf(String.valueOf(value)));
            sysDeptMapper.update(new SysDept().setStatus("0"), where -> where.in(SysDept::getId, parentIds));
        }
        return rows > 0;
    }

    private void updateChildrenAncestors(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = QueryChain.of(sysDeptMapper)
                .and(SysDept::getAncestors, condition -> condition.mysql().findInSet(deptId)).list();
        for (SysDept child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(
                    java.util.regex.Pattern.quote(oldAncestors),
                    java.util.regex.Matcher.quoteReplacement(newAncestors)));
        }
        if (CollUtil.isNotEmpty(children)) {
            sysDeptMapper.updateBatch(children);
        }
    }

    /**
     * 批量删除部门
     */
    @Override
    public Integer deleteByIds(Collection<Long> ids) {
        return sysDeptMapper.deleteByIds(ids);
    }


    /**
     * 查询部门树结构信息
     *
     * @param deptQuery 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<MapTree<Long>> selectDeptTreeList(SysDeptQuery deptQuery) {
        List<SysDeptVo> deptVoList = queryList(deptQuery);
        return buildDeptTreeSelect(deptVoList);
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param deptVoList 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<MapTree<Long>> buildDeptTreeSelect(List<SysDeptVo> deptVoList) {
        if (CollUtil.isEmpty(deptVoList)) {
            return ListUtil.zero();
        }
        // 获取当前列表中每一个节点的parentId，然后在列表中查找是否有id与其parentId对应，若无对应，则表明此时节点列表中，该节点在当前列表中属于顶级节点
        List<MapTree<Long>> treeList = new ArrayList<>();
        for (SysDeptVo d : deptVoList) {
            Long parentId = d.getParentId();
            SysDeptVo sysDeptVo = StreamUtil.findFirst(deptVoList, it -> it.getId().longValue() == parentId);
            if (ObjUtil.isNull(sysDeptVo)) {
                List<MapTree<Long>> trees = TreeBuildUtil.build(deptVoList, parentId, (dept, tree) ->
                        tree.setId(dept.getId())
                                .setParentId(dept.getParentId())
                                .setName(dept.getDeptName())
                                .setWeight(dept.getOrderNum())
                                .putExtra("disabled", "1".equals(dept.getStatus())));
                MapTree<Long> tree = StreamUtil.findFirst(trees, it -> it.getId().longValue() == d.getId());
                treeList.add(tree);
            }
        }
        return treeList;
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId) {
        SysRole role = sysRoleMapper.getById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        return sysDeptMapper.selectDeptListByRoleId(roleId, Boolean.TRUE.equals(role.getDeptCheckStrictly()));
    }

    /**
     * 根据ID查询所有子部门数（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public long selectNormalChildrenDeptById(Long deptId) {
        return QueryChain.of(sysDeptMapper)
               .forSearch(true)
                       .eq(SysDept::getStatus,"0")
                       .and(SysDept::getAncestors, dept -> dept.mysql().findInSet(deptId))
                .count();
    }

    /**
     * 是否存在部门子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId) {
        return sysDeptMapper.exists(where -> where.eq(SysDept::getParentId, deptId));
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId) {
        return sysUserMapper.exists(where -> where.eq(SysUser::getDeptId, deptId));
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param deptQuery 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDeptQuery deptQuery) {
        boolean exist = sysDeptMapper.exists(where -> where
                .eq(SysDept::getDeptName, deptQuery.getDeptName())
                .eq(SysDept::getParentId, deptQuery.getParentId())
                .ne(ObjUtil.isNotNull(deptQuery.getId()), SysDept::getId, deptQuery.getId()));
        return !exist;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId) {
        if (ObjUtil.isNull(deptId)) {
            return;
        }
        if (LoginHelper.isSuperAdmin()) {
            return;
        }
        if (!dataScopeService.checkUserDataScope(LoginHelper.getUserId(), deptId)) {
            throw new ServiceException("没有权限访问部门数据！");
        }
    }
}
