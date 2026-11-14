package com.aurxsiu.bupthelper.helprequest.vo;

/**
 * 需求列表项，见 doc/接口设计-请帮忙模块.md 2.4。
 */
public record RequestSummaryVO(
        Long requestId,
        String title,
        String typeCode,
        String typeName,
        Long regionId,
        String regionName,
        String regionFullName,
        int status,
        String statusName,
        String coverUrl,
        int imageCount,
        int videoCount,
        int responseCount,
        int pendingResponseCount,
        int acceptedResponseCount,
        int myResponseCount,
        boolean canModify,
        boolean canDelete,
        String createTime,
        String updateTime) {
}
