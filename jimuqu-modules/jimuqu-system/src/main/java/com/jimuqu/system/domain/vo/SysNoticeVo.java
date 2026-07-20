package com.jimuqu.system.domain.vo;

import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import com.jimuqu.common.translation.annotation.Trans;
import com.jimuqu.common.translation.enums.TransType;
import com.jimuqu.system.domain.SysNotice;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知公告视图。
 */
@Data
@Accessors(chain = true)
@ResultEntity(SysNotice.class)
@AutoMapper(target = SysNotice.class)
public class SysNoticeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String status;
    private String remark;
    private Long createBy;
    @ResultEntityField(property = "createBy")
    @Trans(type = TransType.USER_NAME, field = "createBy")
    private String createByName;
    private Date createTime;
}
