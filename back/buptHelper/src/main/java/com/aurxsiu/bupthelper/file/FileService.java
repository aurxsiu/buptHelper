package com.aurxsiu.bupthelper.file;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import com.aurxsiu.bupthelper.helprequest.entity.HelpRequestFile;
import com.aurxsiu.bupthelper.helprequest.entity.MediaType;
import com.aurxsiu.bupthelper.helprequest.repository.HelpRequestFileRepository;
import com.aurxsiu.bupthelper.helprequest.vo.AttachmentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 附件上传 / 删除 / 访问，规则见 doc/接口设计-请帮忙模块.md 1.6、6.5。
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");
    private static final long IMAGE_MAX_SIZE = 5L * 1024 * 1024;
    private static final long VIDEO_MAX_SIZE = 100L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 200;

    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "video/mp4", ".mp4",
            "video/webm", ".webm");

    private final HelpRequestFileRepository fileRepository;
    private final UploadProperties uploadProperties;

    public FileService(HelpRequestFileRepository fileRepository, UploadProperties uploadProperties) {
        this.fileRepository = fileRepository;
        this.uploadProperties = uploadProperties;
    }

    /** 附件下载所需的元信息与磁盘路径 */
    public record FileContent(Path path, String contentType, String fileName, long size) {
    }

    @Transactional
    public AttachmentVO upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.PARAM_INVALID, "请选择要上传的文件");
        }

        String contentType = file.getContentType();
        MediaType mediaType = resolveMediaType(contentType);
        long size = file.getSize();
        long maxSize = mediaType == MediaType.VIDEO ? VIDEO_MAX_SIZE : IMAGE_MAX_SIZE;
        if (size <= 0 || size > maxSize) {
            throw new BusinessException(ApiCode.FILE_INVALID, ApiCode.FILE_INVALID.getMessage());
        }

        String extension = EXTENSION_BY_TYPE.get(contentType);
        String originalName = sanitizeFileName(file.getOriginalFilename());
        String storedName = buildStoredName(extension);

        Path target = resolvePath(storedName);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", storedName, e);
            throw new BusinessException(ApiCode.SERVER_ERROR, "文件保存失败，请稍后重试");
        }

        HelpRequestFile entity = new HelpRequestFile();
        entity.setUploaderId(uploaderId);
        entity.setFileName(originalName);
        entity.setStoredName(storedName);
        entity.setMediaType(mediaType);
        entity.setContentType(contentType);
        entity.setFileSize(size);
        entity.setSortNo(0);
        entity.setCreateTime(LocalDateTime.now());
        return AttachmentVO.from(fileRepository.save(entity));
    }

    @Transactional
    public void deleteTemporary(Long fileId, Long userId) {
        HelpRequestFile file = requireFile(fileId);
        if (file.getRequestId() != null) {
            throw new BusinessException(ApiCode.FILE_REFERENCED, ApiCode.FILE_REFERENCED.getMessage());
        }
        if (!file.getUploaderId().equals(userId)) {
            throw new BusinessException(ApiCode.NOT_REQUEST_OWNER, "无权删除该文件");
        }
        fileRepository.delete(file);
        deleteQuietly(resolvePath(file.getStoredName()));
    }

    @Transactional(readOnly = true)
    public FileContent content(Long fileId) {
        HelpRequestFile file = requireFile(fileId);
        Path path = resolvePath(file.getStoredName());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ApiCode.FILE_NOT_FOUND);
        }
        return new FileContent(path, file.getContentType(), file.getFileName(), file.getFileSize());
    }

    private HelpRequestFile requireFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ApiCode.FILE_NOT_FOUND));
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(ApiCode.FILE_INVALID, "无法识别文件类型");
        }
        if (IMAGE_TYPES.contains(contentType)) {
            return MediaType.IMAGE;
        }
        if (VIDEO_TYPES.contains(contentType)) {
            return MediaType.VIDEO;
        }
        throw new BusinessException(ApiCode.FILE_INVALID, "仅支持图片（JPG/PNG/WebP/GIF）或视频（MP4/WebM）文件");
    }

    private String buildStoredName(String extension) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        return year + "/" + month + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private Path resolvePath(String storedName) {
        Path base = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
        Path resolved = base.resolve(storedName).normalize();
        if (!resolved.startsWith(base)) {
            throw new BusinessException(ApiCode.FILE_NOT_FOUND);
        }
        return resolved;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String cleaned = name.replace('\\', '/');
        int index = cleaned.lastIndexOf('/');
        if (index >= 0) {
            cleaned = cleaned.substring(index + 1);
        }
        if (cleaned.length() > MAX_FILE_NAME_LENGTH) {
            cleaned = cleaned.substring(cleaned.length() - MAX_FILE_NAME_LENGTH);
        }
        return cleaned.isBlank() ? "file" : cleaned;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败，忽略：{}", path, e);
        }
    }
}
