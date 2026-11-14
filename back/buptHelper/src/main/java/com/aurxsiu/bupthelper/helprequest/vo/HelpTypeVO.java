package com.aurxsiu.bupthelper.helprequest.vo;

import com.aurxsiu.bupthelper.helprequest.entity.HelpType;

/**
 * 帮忙类型，见 doc/接口设计-请帮忙模块.md 2.1。
 */
public record HelpTypeVO(String typeCode, String typeName, int sortNo) {

    public static HelpTypeVO from(HelpType type) {
        return new HelpTypeVO(type.getTypeCode(), type.getTypeName(), type.getSortNo());
    }
}
