package com.aurxsiu.bupthelper.helprequest.vo;

/**
 * 接受 / 拒绝响应的结果，见 doc/接口设计-请帮忙模块.md 4.9 / 4.10。
 */
public record ResponseStatusVO(Long responseId, int status, String statusName) {
}
