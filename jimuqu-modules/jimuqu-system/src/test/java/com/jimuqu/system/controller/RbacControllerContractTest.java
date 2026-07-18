package com.jimuqu.system.controller;

import com.jimuqu.system.domain.bo.SysRoleBo;
import org.noear.solon.annotation.Body;
import org.noear.solon.annotation.Mapping;

import java.lang.reflect.Method;

class RbacControllerContractTest {

    public static void main(String[] args) throws Exception {
        Method optionselect = SysUserController.class.getMethod("optionselect", Long[].class, Long.class);
        assert "/optionselect".equals(optionselect.getAnnotation(Mapping.class).value());

        Method addRole = SysRoleController.class.getMethod("add", SysRoleBo.class);
        assert addRole.getParameters()[0].isAnnotationPresent(Body.class);
    }
}
