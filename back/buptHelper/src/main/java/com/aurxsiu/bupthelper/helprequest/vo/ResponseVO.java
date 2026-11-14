package com.aurxsiu.bupthelper.helprequest.vo;

import java.util.List;

/**
 * “我来也”响应，见 doc/接口设计-请帮忙模块.md 2.6。
 */
public record ResponseVO(
        Long responseId,
        Long requestId,
        UserBriefVO responder,
        String description,
        List<AttachmentVO> attachments,
        int status,
        String statusName,
        boolean canAccept,
        boolean canReject,
        boolean canModify,
        boolean canDelete,
        String createTime,
        String updateTime) {
}
