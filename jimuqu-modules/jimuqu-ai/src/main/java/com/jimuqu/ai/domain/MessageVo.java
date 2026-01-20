package com.jimuqu.ai.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天消息
 *
 * @author chengliang
 * @date 2025/12/28
 */
@Data
@NoArgsConstructor
public class MessageVo implements Serializable {

    /**
     * 消息内容
     */
    private String content;

    /**
     * 文件列表
     */
    private List<String> fileList;

}
