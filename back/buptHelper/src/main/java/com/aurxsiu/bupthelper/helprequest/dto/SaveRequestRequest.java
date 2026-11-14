package com.aurxsiu.bupthelper.helprequest.dto;

import java.util.List;

/**
 * 发布 / 修改需求请求，见 doc/接口设计-请帮忙模块.md 4.5 / 4.6。
 */
public record SaveRequestRequest(
        String title,
        String typeCode,
        Long regionId,
        String description,
        List<Long> attachmentIds) {
}
