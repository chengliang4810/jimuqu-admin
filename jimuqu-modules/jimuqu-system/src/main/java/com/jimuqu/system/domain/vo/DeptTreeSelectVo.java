package com.jimuqu.system.domain.vo;

import lombok.Data;
import cn.hutool.core.lang.tree.Tree;

import java.util.List;

/**
 * 角色部门列表树信息
 *
 * @author Michelle.Chung
 */
@Data
public class DeptTreeSelectVo {

    /**
     * 选中部门列表
     */
    private List<Long> checkedKeys;

    /**
     * 下拉树结构列表
     */
    private List<Tree<Long>> depts;

}
