package com.jimuqu.common.core.service;

/**
 * 通用 OSS服务
 *
 * @author Lion Li,chengliang4810
 */
public interface OssService {

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    String selectUrlByIds(String ossIds);

}
