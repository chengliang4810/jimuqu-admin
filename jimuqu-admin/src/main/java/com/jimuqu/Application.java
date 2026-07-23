package com.jimuqu;

import cn.dev33.satoken.annotation.SaIgnore;
import com.jimuqu.common.core.domain.R;
import org.dromara.autotable.solon.annotation.EnableAutoTable;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.SolonMain;
import org.noear.solon.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 *
 * @author chengliang
 * @since 2024/02/26
 */
@SolonMain
@Controller
@EnableAutoTable
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        Solon.start(Application.class, args);
    }

    /**
     * 访问首页时返回引导提示。
     */
    @Get
    @Mapping
    @SaIgnore
    public R<String> index() {
        return R.data("欢迎使用" + Solon.cfg().get("solon.app.name", "jimuqu-admin")
                + "后台管理框架，请通过前端地址访问。");
    }

}
