package com.jimuqu.system.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Bell 消息对象。
 */
@Data
@Accessors(chain = true)
public class SysMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String messageId;
    private String category;
    private String type;
    private String source;
    private String title;
    private String message;
    private String content;
    private Object data;
    private String path;
    private BigDecimal timestamp;
    private Date createTime;
}
