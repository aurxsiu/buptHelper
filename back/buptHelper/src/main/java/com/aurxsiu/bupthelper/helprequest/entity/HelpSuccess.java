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
 * “好帮手”响应成功明细（《需求说明.md》三.5），见 doc/接口设计-请帮忙模块.md 6.2。
 */
@Entity
@Table(name = "help_success")
@Getter
@Setter
@NoArgsConstructor
public class HelpSuccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "success_id")
    private Long successId;

    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;

    @Column(name = "publish_user_id", nullable = false)
    private Long publishUserId;

    @Column(name = "response_id", nullable = false, unique = true)
    private Long responseId;

    @Column(name = "responder_user_id", nullable = false)
    private Long responderUserId;

    @Column(name = "accept_time", nullable = false)
    private LocalDateTime acceptTime;
}
