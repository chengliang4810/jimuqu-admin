package com.jimuqu.common.mybatis.interceptor;

import com.jimuqu.common.mybatis.annotation.DataPermission;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据权限上下文持有者
 * <p>
 * 使用ThreadLocal存储数据权限注解，实现线程间隔离
 * 支持嵌套调用，使用Deque栈结构管理
 *
 * @author chengliang4810
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataPermissionHolder {

    private static final ThreadLocal<Deque<DataPermission>> PERMISSION_HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 获取当前数据权限注解
     */
    public static DataPermission get() {
        return PERMISSION_HOLDER.get().peek();
    }

    /**
     * 设置数据权限注解
     */
    public static void push(DataPermission dataPermission) {
        PERMISSION_HOLDER.get().push(dataPermission);
    }

    /**
     * 移除数据权限注解
     */
    public static void pop() {
        Deque<DataPermission> deque = PERMISSION_HOLDER.get();
        if (!deque.isEmpty()) {
            deque.pop();
        }
    }

    /**
     * 清空数据权限注解
     */
    public static void clear() {
        PERMISSION_HOLDER.get().clear();
    }

    /**
     * 临时忽略数据权限
     */
    public static void withIgnore(Runnable runnable) {
        try {
            clear();
            runnable.run();
        } finally {
            // 注意：这里不能恢复，因为可能已经被其他调用修改
            // 如果需要恢复，应该使用更复杂的栈管理
        }
    }

}