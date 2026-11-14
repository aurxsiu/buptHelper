package com.aurxsiu.bupthelper.helprequest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * “请帮忙” / “我来也”附件（《需求说明.md》三.3、三.4 的图片 / 视频文件），见 doc/接口设计-请帮忙模块.md 6.2。
 * 附件可绑定到需求（requestId）或响应（responseId），两者均为空表示尚未绑定的临时文件。
 */
@Entity
@Table(name = "help_request_file")
@Getter
@Setter
@NoArgsConstructor
public class HelpRequestFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "response_id")
    private Long responseId;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 相对 upload.dir 的存储路径，形如 yyyy/MM/{uuid}.{ext} */
    @Column(name = "stored_name", nullable = false, length = 100)
    private String storedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "sort_no", nullable = false)
    private int sortNo;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
}
