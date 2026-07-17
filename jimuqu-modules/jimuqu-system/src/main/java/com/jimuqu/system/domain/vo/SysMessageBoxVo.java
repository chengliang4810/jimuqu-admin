package com.jimuqu.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bell 消息盒子。
 */
@Data
public class SysMessageBoxVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<SysMessageVo> systemList = new ArrayList<>();
    private List<SysMessageVo> noticeList = new ArrayList<>();
}
