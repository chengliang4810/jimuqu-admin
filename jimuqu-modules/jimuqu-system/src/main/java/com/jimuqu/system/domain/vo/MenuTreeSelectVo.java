package com.jimuqu.system.domain.vo;

import lombok.Data;
import cn.hutool.v7.core.tree.MapTree;

import java.util.List;

/**
 * 角色菜单列表树信息
 *
 * @author Michelle.Chung
 */
@Data
public class MenuTreeSelectVo {

    /**
     * 选中菜单列表
     */
    private List<Long> checkedKeys;

    /**
     * 菜单下拉树结构列表
     */
    private List<MapTree<Long>> menus;

}
