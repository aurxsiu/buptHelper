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
 * “我来也”响应（《需求说明.md》三.4），见 doc/接口设计-请帮忙模块.md 1.5 / 6.2。
 */
@Entity
@Table(name = "help_response")
@Getter
@Setter
@NoArgsConstructor
public class HelpResponse {

    /** 状态：待接受 */
    public static final int STATUS_PENDING = 0;
    /** 状态：同意 */
    public static final int STATUS_ACCEPTED = 1;
    /** 状态：拒绝 */
    public static final int STATUS_REJECTED = 2;
    /** 状态：取消 */
    public static final int STATUS_CANCELLED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long responseId;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "responder_id", nullable = false)
    private Long responderId;

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
            case STATUS_PENDING -> "待接受";
            case STATUS_ACCEPTED -> "同意";
            case STATUS_REJECTED -> "拒绝";
            case STATUS_CANCELLED -> "取消";
            default -> "未知状态";
        };
    }
}
