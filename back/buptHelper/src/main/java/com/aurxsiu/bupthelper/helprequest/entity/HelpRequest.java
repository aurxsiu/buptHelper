package com.aurxsiu.bupthelper.helprequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * “请帮忙”需求（《需求说明.md》三.3），见 doc/接口设计-请帮忙模块.md 6.2。
 */
@Entity
@Table(name = "help_request")
@Getter
@Setter
@NoArgsConstructor
public class HelpRequest {

    /** 状态：已发布 */
    public static final int STATUS_PUBLISHED = 0;
    /** 状态：已取消（用户删除后置为该状态） */
    public static final int STATUS_CANCELLED = -1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "publish_user_id", nullable = false)
    private Long publishUserId;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "type_code", nullable = false, length = 30)
    private String typeCode;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public static String statusName(int status) {
        return switch (status) {
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_CANCELLED -> "已取消";
            default -> "未知状态";
        };
    }
}
