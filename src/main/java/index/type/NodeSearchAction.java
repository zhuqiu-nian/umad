package index.type;

import index.structure.Node;

/**
 * 枚举类，通过该类标记节点的处理方式。该枚举可选的属性有[RESULTNONE, RESULTUNKNOWN, RESULTALL]。
 * <p>
 * 通过枚举类{@link NodeSearchAction}标记{@link Node}的每个子节点的处理方法。
 * “RESULTNONE”表示该节点被剪枝，稍后的搜索不进入该孩子。
 * “RESULTUNKNOWN”表示该节点不能排除，稍后的搜索进入该孩子。
 * “RESULTALL”表示该节点的所有数据都应该被添加到结果集中。
 * </p>
 */
public enum NodeSearchAction
{
    /**
     * 表示该节点被剪枝，稍后的搜索不进入该孩子
     */
    RESULTNONE,
    /**
     * 表示该节点不能排除，稍后的搜索进入该孩子
     */
    RESULTUNKNOWN,
    /**
     * 表示该节点的所有数据都应该被添加到结果集中
     */
    RESULTALL,
    /**
     * 表示该节点的数据完全在查询超立方体中，需要进行一次线性扫描才可以判断
     */
    RESULTNEEDLINERSCAN
}
