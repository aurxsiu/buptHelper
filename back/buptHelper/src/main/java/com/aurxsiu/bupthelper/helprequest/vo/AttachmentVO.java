package com.aurxsiu.bupthelper.helprequest.vo;

import com.aurxsiu.bupthelper.helprequest.entity.HelpRequestFile;

/**
 * 附件，见 doc/接口设计-请帮忙模块.md 2.3。
 */
public record AttachmentVO(
        Long fileId,
        String fileName,
        String mediaType,
        String contentType,
        long size,
        String url,
        int sortNo) {

    public static AttachmentVO from(HelpRequestFile file) {
        return new AttachmentVO(
                file.getFileId(),
                file.getFileName(),
                file.getMediaType().name(),
                file.getContentType(),
                file.getFileSize(),
                "/api/files/" + file.getFileId() + "/content",
                file.getSortNo());
    }
}
