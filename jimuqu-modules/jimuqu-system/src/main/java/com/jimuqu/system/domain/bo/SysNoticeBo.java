package com.jimuqu.system.domain.bo;

import lombok.Data;
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
    private String noticeTitle;

    private String noticeType;
    private String noticeContent;
    private String status;
    private String remark;
}
