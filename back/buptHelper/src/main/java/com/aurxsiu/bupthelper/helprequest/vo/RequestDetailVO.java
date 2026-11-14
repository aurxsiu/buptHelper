package com.aurxsiu.bupthelper.helprequest.vo;

import java.util.List;

/**
 * 需求详情，见 doc/接口设计-请帮忙模块.md 2.5。
 */
public record RequestDetailVO(
        Long requestId,
        String title,
        String typeCode,
        String typeName,
        Long regionId,
        String regionName,
        String regionFullName,
        int status,
        String statusName,
        String description,
        String coverUrl,
        int imageCount,
        int videoCount,
        int responseCount,
        int pendingResponseCount,
        int acceptedResponseCount,
        int myResponseCount,
        Long myResponseId,
        boolean canModify,
        boolean canDelete,
        boolean mine,
        UserBriefVO publisher,
        List<AttachmentVO> attachments,
        String createTime,
        String updateTime) {
}
