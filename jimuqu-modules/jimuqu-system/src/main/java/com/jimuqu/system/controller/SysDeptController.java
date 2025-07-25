package com.jimuqu.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jimuqu.common.core.checker.Assert;
import com.jimuqu.common.core.constant.UserConstants;
import com.jimuqu.common.core.domain.R;
import com.jimuqu.common.core.utils.StringUtil;
import com.jimuqu.common.core.validate.group.AddGroup;
import com.jimuqu.common.core.validate.group.UpdateGroup;
import com.jimuqu.common.log.annotation.Log;
import com.jimuqu.common.log.enums.BusinessType;
import com.jimuqu.common.web.core.BaseController;
import com.jimuqu.system.domain.bo.SysDeptBo;
import com.jimuqu.system.domain.query.SysDeptQuery;
import com.jimuqu.system.domain.vo.SysDeptVo;
import com.jimuqu.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import cn.hutool.v7.core.convert.ConvertUtil;
import cn.hutool.v7.core.tree.MapTree;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.validation.annotation.NoRepeatSubmit;
import org.noear.solon.validation.annotation.NotNull;
import org.noear.solon.validation.annotation.Validated;

import java.util.List;

/**
 * 部门 Controller
 *
 * @author chengliang4810
 * @since 2025-06-04
 */
@Post
@Controller
@RequiredArgsConstructor
@Mapping("/system/dept")
public class SysDeptController extends BaseController {

    private final SysDeptService sysDeptService;

    /**
     * 查询部门列表
     */
    @Get
    @Mapping("/list")
    @SaCheckPermission("system:dept:list")
    public List<SysDeptVo> list(SysDeptQuery query) {
        return sysDeptService.queryList(query);
    }

    /**
     * 获取部门树列表
     */
    @Get
    @Mapping("/tree" )
    @SaCheckPermission("system:dept:list" )
    public R<List<MapTree<Long>>> deptTree(SysDeptQuery dept) {
        return R.ok(sysDeptService.selectDeptTreeList(dept));
    }

    /**
     * 查询部门列表（排除节点）
     *
     * @param deptId 部门ID
     */
    @Get
    @Mapping("/tree/exclude/{deptId}") // @PathVariable(value = "deptId", required = false)
    @SaCheckPermission("system:dept:list" )
    public List<MapTree<Long>> deptTreeExcludeChild(Long deptId) {
        List<SysDeptVo> deptVoList = sysDeptService.queryList(new SysDeptQuery());
        deptVoList.removeIf(d -> d.getId().equals(deptId) || StringUtil.splitList(d.getAncestors()).contains(ConvertUtil.toStr(deptId)));
        return sysDeptService.buildDeptTreeSelect(deptVoList);
    }

    /**
     * 查询部门列表（排除节点）
     *
     * @param deptId 部门ID
     */
    @Get
    @Mapping("/list/exclude/{deptId}") // @PathVariable(value = "deptId", required = false)
    @SaCheckPermission("system:dept:list" )
    public R<List<SysDeptVo>> excludeChild(Long deptId) {
        List<SysDeptVo> deptVoList = sysDeptService.queryList(new SysDeptQuery());
        deptVoList.removeIf(d -> d.getId().equals(deptId) || StringUtil.splitList(d.getAncestors()).contains(ConvertUtil.toStr(deptId)));
        return R.ok(deptVoList);
    }

    /**
     * 获取部门详细信息
     *
     * @param id 部门主键
     */
    @Get
    @Mapping("/{id}")
    @SaCheckPermission("system:dept:query")
    public SysDeptVo getInfo(@NotNull(message = "部门主键不能为空") Long id) {
        sysDeptService.checkDeptDataScope(id);
        return sysDeptService.queryById(id);
    }

    /**
     * 新增部门
     */
    @Mapping("/add")
    @NoRepeatSubmit
    @SaCheckPermission("system:dept:add")
    @Log(title = "新增部门", businessType = BusinessType.ADD)
    public Long add(@Validated(AddGroup.class) SysDeptBo bo) {
        boolean checkDeptNameUnique = sysDeptService.checkDeptNameUnique(new SysDeptQuery().setParentId(bo.getParentId()).setDeptName(bo.getDeptName()));
        Assert.isTrue(checkDeptNameUnique, "新增部门'{}'失败，部门名称已存在", bo.getDeptName());
        boolean result = sysDeptService.insertByBo(bo);
        Assert.isTrue(result, "新增部门失败");
        return bo.getId();
    }

    /**
     * 更新部门
     */
    @NoRepeatSubmit
    @Mapping("/update")
    @SaCheckPermission("system:dept:update")
    @Log(title = "更新部门", businessType = BusinessType.UPDATE)
    public void edit(@Validated(UpdateGroup.class) SysDeptBo bo) {
        Long deptId = bo.getId();
        sysDeptService.checkDeptDataScope(deptId);
        if (!sysDeptService.checkDeptNameUnique(new SysDeptQuery().setParentId(bo.getParentId()).setDeptName(bo.getDeptName()).setId(deptId))) {
            Assert.fail("修改部门'{}'失败，部门名称已存在", bo.getDeptName());
        } else if (bo.getParentId().equals(deptId)) {
            Assert.fail("修改部门'{}'失败，上级部门不能是自己", bo.getDeptName());
        } else if (StringUtil.equals(UserConstants.DEPT_DISABLE, bo.getStatus())) {
            if (sysDeptService.selectNormalChildrenDeptById(deptId) > 0) {
                Assert.fail("该部门包含未停用的子部门!");
            } else if (sysDeptService.checkDeptExistUser(deptId)) {
                Assert.fail("该部门下存在已分配用户，不能禁用!");
            }
        }
        boolean result = sysDeptService.updateByBo(bo);
        Assert.isTrue(result, "更新部门失败");
    }

    /**
     * 删除部门
     */
    @Mapping("/delete/{id}")
    @SaCheckPermission("system:dept:delete")
    @Log(title = "删除部门", businessType = BusinessType.DELETE)
    public Integer delete(@NotNull(message = "主键不能为空") Long id) {
        Assert.isFalse(sysDeptService.hasChildByDeptId(id), "存在下级部门,不允许删除");
        Assert.isFalse(sysDeptService.checkDeptExistUser(id), "部门存在用户,不允许删除");
        sysDeptService.checkDeptDataScope(id);
        Integer num = sysDeptService.deleteByIds(List.of(id));
        Assert.gtZero(num, "删除部门失败");
        return num;
    }

}