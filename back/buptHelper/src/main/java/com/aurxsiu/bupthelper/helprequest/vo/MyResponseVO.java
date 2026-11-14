package com.aurxsiu.bupthelper.helprequest.vo;

import java.util.List;

/**
 * “我的响应”列表项：响应信息 + 所属需求摘要，见 doc/接口设计-我来也模块.md 2.4。
 */
public record MyResponseVO(
        Long responseId,
        Long requestId,
        int status,
        String statusName,
        String description,
        List<AttachmentVO> attachments,
        boolean canModify,
        boolean canDelete,
        String createTime,
        String updateTime,
        RequestSummaryVO request) {
}
