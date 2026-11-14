package com.aurxsiu.bupthelper.helprequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 帮忙类型（《需求说明.md》三.3 的服务类型编码表），见 doc/接口设计-请帮忙模块.md 1.5。
 */
@Entity
@Table(name = "help_type")
@Getter
@Setter
@NoArgsConstructor
public class HelpType {

    @Id
    @Column(name = "type_code", nullable = false, length = 30)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 30)
    private String typeName;

    @Column(name = "sort_no", nullable = false)
    private int sortNo;
}
