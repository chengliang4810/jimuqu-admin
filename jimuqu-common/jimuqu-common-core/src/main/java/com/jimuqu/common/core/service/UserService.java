package com.jimuqu.common.core.service;

/**
 * 通用 用户服务
 *
 * @author Lion Li,chengliang4810
 */
public interface UserService {

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    String selectUserNameById(Long userId);

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    String selectNicknameById(Long userId);

}
