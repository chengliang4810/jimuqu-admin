package com.jimuqu.system.domain;

import cn.xbatis.core.logicDelete.LogicDeleteSwitch;
import com.jimuqu.Application;
import com.jimuqu.system.domain.bo.SysPostBo;
import com.jimuqu.system.mapper.SysDeptMapper;
import com.jimuqu.system.mapper.SysPostMapper;
import com.jimuqu.system.service.SysPostService;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SolonTest(value = Application.class, env = "test", debug = false)
public class SysPostNameUniquenessIntegrationTest {

    @Inject
    private SysDeptMapper deptMapper;
    @Inject
    private SysPostMapper postMapper;
    @Inject
    private SysPostService postService;

    @Test
    void postNameIsUniqueWithinDepartmentOnly() {
        String suffix = Long.toUnsignedString(System.nanoTime(), 36);
        List<Long> deptIds = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        try {
            SysDept firstDept = dept("岗位部门一-" + suffix);
            SysDept secondDept = dept("岗位部门二-" + suffix);
            deptMapper.save(firstDept);
            deptMapper.save(secondDept);
            deptIds.add(firstDept.getId());
            deptIds.add(secondDept.getId());

            SysPost firstPost = post(firstDept.getId(), "同名岗位-" + suffix, "same-post-a-" + suffix);
            postMapper.save(firstPost);
            postIds.add(firstPost.getPostId());

            assertFalse(postService.checkPostNameUnique(new SysPostBo()
                    .setDeptId(firstDept.getId()).setPostName(firstPost.getPostName())));
            assertTrue(postService.checkPostNameUnique(new SysPostBo()
                    .setDeptId(secondDept.getId()).setPostName(firstPost.getPostName())));

            SysPost secondPost = post(secondDept.getId(), firstPost.getPostName(), "same-post-b-" + suffix);
            postMapper.save(secondPost);
            postIds.add(secondPost.getPostId());
            assertTrue(postService.checkPostNameUnique(new SysPostBo()
                    .setPostId(secondPost.getPostId())
                    .setDeptId(secondDept.getId())
                    .setPostName(secondPost.getPostName())));
        } finally {
            try (LogicDeleteSwitch ignored = LogicDeleteSwitch.with(false)) {
                postMapper.deleteByIds(postIds);
                deptMapper.deleteByIds(deptIds);
            }
        }
    }

    private static SysDept dept(String name) {
        return new SysDept().setParentId(0L).setAncestors("0").setDeptName(name)
                .setOrderNum(99).setStatus("0").setDelFlag("0");
    }

    private static SysPost post(Long deptId, String name, String code) {
        return new SysPost().setDeptId(deptId).setPostName(name).setPostCode(code)
                .setPostSort(99).setStatus("0").setDelFlag("0");
    }
}
