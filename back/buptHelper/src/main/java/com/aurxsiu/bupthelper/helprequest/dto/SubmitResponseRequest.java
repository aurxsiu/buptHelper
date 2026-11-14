package com.aurxsiu.bupthelper.helprequest.dto;

import java.util.List;

/**
 * 提交 / 修改“我来也”响应请求，见 doc/接口设计-我来也模块.md 4.1 / 4.4。
 */
public record SubmitResponseRequest(Long requestId, String description, List<Long> attachmentIds) {
}
