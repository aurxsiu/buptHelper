package com.aurxsiu.bupthelper.helprequest.vo;

import java.util.List;

/**
 * “我来也”响应详情：响应信息 + 响应者 + 所属需求摘要，见 doc/接口设计-我来也模块.md 2.5。
 */
public record ResponseDetailVO(
        Long responseId,
        Long requestId,
        int status,
        String statusName,
        String description,
        List<AttachmentVO> attachments,
        boolean canAccept,
        boolean canReject,
        boolean canModify,
        boolean canDelete,
        String createTime,
        String updateTime,
        UserBriefVO responder,
        RequestSummaryVO request) {
}
