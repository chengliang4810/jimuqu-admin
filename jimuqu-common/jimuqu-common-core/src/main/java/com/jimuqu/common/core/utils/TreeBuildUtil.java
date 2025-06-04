package com.jimuqu.common.core.utils;

import com.jimuqu.common.core.utils.reflect.ReflectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.tree.MapTree;
import org.dromara.hutool.core.tree.TreeNodeConfig;
import org.dromara.hutool.core.tree.TreeUtil;
import org.dromara.hutool.core.tree.parser.NodeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 扩展 hutool TreeUtil 封装系统树构建
 *
 * @author Lion Li,chengliang4810
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeBuildUtil extends TreeUtil {

    /**
     * 根据前端定制差异化字段
     */
    public static final TreeNodeConfig DEFAULT_CONFIG = TreeNodeConfig.DEFAULT_CONFIG.setNameKey("label");

    /**
     * 构建树形结构
     *
     * @param <T>        输入节点的类型
     * @param <K>        节点ID的类型
     * @param list       节点列表，其中包含了要构建树形结构的所有节点
     * @param nodeParser 解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<MapTree<K>> build(List<T> list, NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        K k = ReflectUtil.invokeGetter(list.get(0), "parentId");
        return TreeUtil.build(list, k, DEFAULT_CONFIG, nodeParser);
    }

    /**
     * 构建树形结构
     *
     * @param <T>        输入节点的类型
     * @param <K>        节点ID的类型
     * @param parentId   顶级节点
     * @param list       节点列表，其中包含了要构建树形结构的所有节点
     * @param nodeParser 解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<MapTree<K>> build(List<T> list, K parentId, NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return TreeUtil.build(list, parentId, DEFAULT_CONFIG, nodeParser);
    }

    /**
     * 获取节点列表中所有节点的叶子节点
     *
     * @param <K>   节点ID的类型
     * @param nodes 节点列表
     * @return 包含所有叶子节点的列表
     */
    public static <K> List<MapTree<K>> getLeafNodes(List<MapTree<K>> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return new ArrayList<>();
        }
        return nodes.stream()
                .flatMap(TreeBuildUtil::extractLeafNodes)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定节点下的所有叶子节点
     *
     * @param <K>  节点ID的类型
     * @param node 要查找叶子节点的根节点
     * @return 包含所有叶子节点的列表
     */
    private static <K> Stream<MapTree<K>> extractLeafNodes(MapTree<K> node) {
        if (!node.hasChild()) {
            return Stream.of(node);
        } else {
            // 递归调用，获取所有子节点的叶子节点
            return node.getChildren().stream()
                    .flatMap(TreeBuildUtil::extractLeafNodes);
        }
    }
}
