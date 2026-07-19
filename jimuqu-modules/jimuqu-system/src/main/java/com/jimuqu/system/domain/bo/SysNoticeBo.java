package com.jimuqu.system.domain.bo;

import lombok.Data;
import com.jimuqu.common.core.xss.Xss;
import org.noear.solon.validation.annotation.Length;
import org.noear.solon.validation.annotation.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知公告业务对象。
 */
@Data
public class SysNoticeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;

    @NotBlank(message = "公告标题不能为空")
    @Length(max = 50, message = "公告标题不能超过50个字符")
    @Xss(message = "公告标题不能包含脚本字符")
    private String noticeTitle;

    private String noticeType;
    private String noticeContent;
    private String status;
    private String remark;
}
