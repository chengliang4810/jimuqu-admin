package com.jimuqu.common.mybatis.core.mapper;

import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import cn.xbatis.core.sql.executor.Where;
import com.jimuqu.common.core.utils.MapstructUtil;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.util.ObjUtil;
import org.noear.solon.core.util.GenericUtil;

import java.io.Serializable;
import java.util.List;

/**
 * 自定义 Mapper 接口, 实现 自定义扩展
 *
 * @author chengliang
 * @date 2025/04/03
 */
public interface BaseMapperPlus<T, V> extends MybatisMapper<T> {

    /**
     * 日志
     */
    Log log = LogFactory.getLog(BaseMapperPlus.class);

    /**
     * 获取当前实例对象关联的泛型类型 V 的 Class 对象
     *
     * @return 返回当前实例对象关联的泛型类型 V 的 Class 对象
     */
    default Class<V> currentVoClass() {
        return (Class<V>) GenericUtil.resolveTypeArguments(this.getClass(), BaseMapperPlus.class)[1];
    }

    /**
     * 获取当前实例对象关联的泛型类型 T 的 Class 对象
     *
     * @return 返回当前实例对象关联的泛型类型 T 的 Class 对象
     */
    default Class<T> currentModelClass() {
        return (Class<T>) GenericUtil.resolveTypeArguments(this.getClass(), BaseMapperPlus.class)[0];
    }

    /**
     * 根据ID查询单个VO对象
     *
     * @param id 主键ID
     * @return 查询到的单个VO对象
     */
    default V getVoById(Serializable id) {
        return getById(id, this.currentVoClass());
    }

    /**
     * 根据ID查询单个VO对象并将其转换为指定的VO类
     *
     * @param id      主键ID
     * @param voClass 要转换的VO类的Class对象
     * @param <C>     VO类的类型
     * @return 查询到的单个VO对象，经过转换为指定的VO类后返回
     */
    default  <C, ID extends Serializable> C getById(ID id, Class<C> voClass) {
        T obj = this.getById(id);
        if (ObjUtil.isNull(obj)) {
            return null;
        }
        return MapstructUtil.convert(obj, voClass);
    }

    /**
     * 列表查询
     *
     * @param query 查询query
     * @return 返回查询列表
     */
    default <C> List<C> list(Where query, Class<C> voClass){
        List<T> list = this.list(query);
        if (CollUtil.isEmpty(list)) {
            return ListUtil.zero();
        }
        return MapstructUtil.convert(list, voClass);
    }

}
