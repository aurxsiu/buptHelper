package com.aurxsiu.bupthelper.file;

import com.aurxsiu.bupthelper.common.ApiResponse;
import com.aurxsiu.bupthelper.helprequest.vo.AttachmentVO;
import com.aurxsiu.bupthelper.security.AuthInterceptor;
import com.aurxsiu.bupthelper.security.AuthUser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 附件接口，见 doc/接口设计-请帮忙模块.md 4.12 ~ 4.14。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ApiResponse<AttachmentVO> upload(@RequestParam("file") MultipartFile file,
                                            @RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser) {
        return ApiResponse.ok("上传成功", fileService.upload(file, currentUser.userId()));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long fileId,
                                    @RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser) {
        fileService.deleteTemporary(fileId, currentUser.userId());
        return ApiResponse.ok("附件已删除", null);
    }

    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long fileId) {
        FileService.FileContent content = fileService.content(fileId);

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.size())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(content.path()));
    }
}
