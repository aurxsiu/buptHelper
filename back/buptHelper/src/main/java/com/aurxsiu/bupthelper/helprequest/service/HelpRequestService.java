package com.aurxsiu.bupthelper.helprequest.service;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import com.aurxsiu.bupthelper.helprequest.dto.SaveRequestRequest;
import com.aurxsiu.bupthelper.helprequest.entity.HelpRequest;
import com.aurxsiu.bupthelper.helprequest.entity.HelpRequestFile;
import com.aurxsiu.bupthelper.helprequest.entity.HelpResponse;
import com.aurxsiu.bupthelper.helprequest.entity.HelpType;
import com.aurxsiu.bupthelper.helprequest.entity.MediaType;
import com.aurxsiu.bupthelper.helprequest.entity.Region;
import com.aurxsiu.bupthelper.helprequest.repository.HelpRequestFileRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpRequestRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpResponseRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpSuccessRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpTypeRepository;
import com.aurxsiu.bupthelper.helprequest.repository.RegionRepository;
import com.aurxsiu.bupthelper.helprequest.vo.AttachmentVO;
import com.aurxsiu.bupthelper.helprequest.vo.HelpTypeVO;
import com.aurxsiu.bupthelper.helprequest.vo.PageResult;
import com.aurxsiu.bupthelper.helprequest.vo.RegionVO;
import com.aurxsiu.bupthelper.helprequest.vo.RequestDetailVO;
import com.aurxsiu.bupthelper.helprequest.vo.RequestSummaryVO;
import com.aurxsiu.bupthelper.helprequest.vo.ResponseVO;
import com.aurxsiu.bupthelper.helprequest.vo.UserBriefVO;
import com.aurxsiu.bupthelper.helprequest.vo.VoUtils;
import com.aurxsiu.bupthelper.user.entity.User;
import com.aurxsiu.bupthelper.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * “请帮忙”需求业务：查询、发布、修改、删除，响应查看与接受 / 拒绝，字典。
 * 规则见 doc/接口设计-请帮忙模块.md 第 4、6 章。
 */
@Service
public class HelpRequestService {

    private static final String USER_TYPE_ADMIN = "ADMIN";

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 1000;
    private static final int KEYWORD_MAX_LENGTH = 50;
    private static final int MAX_ATTACHMENTS = 7;
    private static final int MAX_IMAGES = 6;
    private static final int MAX_VIDEOS = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final HelpRequestRepository requestRepository;
    private final HelpRequestFileRepository fileRepository;
    private final HelpResponseRepository responseRepository;
    private final HelpSuccessRepository successRepository;
    private final HelpTypeRepository typeRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;

    public HelpRequestService(HelpRequestRepository requestRepository,
                              HelpRequestFileRepository fileRepository,
                              HelpResponseRepository responseRepository,
                              HelpSuccessRepository successRepository,
                              HelpTypeRepository typeRepository,
                              RegionRepository regionRepository,
                              UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.fileRepository = fileRepository;
        this.responseRepository = responseRepository;
        this.successRepository = successRepository;
        this.typeRepository = typeRepository;
        this.regionRepository = regionRepository;
        this.userRepository = userRepository;
    }

    // ---------- 查询 ----------

    @Transactional(readOnly = true)
    public PageResult<RequestSummaryVO> list(Long currentUserId, String scope, Integer status, String typeCode,
                                             Long regionId, String keyword, Integer page, Integer pageSize, String sort) {
        boolean all = "all".equalsIgnoreCase(scope);

        if (status != null && status != HelpRequest.STATUS_PUBLISHED && status != HelpRequest.STATUS_CANCELLED) {
            throw paramInvalid(Map.of("status", "状态参数不合法"));
        }

        String normalizedType = trimToNull(typeCode);
        if (normalizedType != null && !typeRepository.existsById(normalizedType)) {
            throw paramInvalid(Map.of("typeCode", "帮忙类型不存在"));
        }
        if (regionId != null && !regionRepository.existsById(regionId)) {
            throw paramInvalid(Map.of("regionId", "地域不存在"));
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > KEYWORD_MAX_LENGTH) {
            throw paramInvalid(Map.of("keyword", "关键词长度不能超过 50 个字符"));
        }

        int pageNumber = page == null ? 1 : page;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (pageNumber < 1) {
            throw paramInvalid(Map.of("page", "页码不能小于 1"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw paramInvalid(Map.of("pageSize", "每页条数需在 1~50 之间"));
        }

        Specification<HelpRequest> spec = buildSpec(currentUserId, all, status, normalizedType, regionId, normalizedKeyword);
        Page<HelpRequest> result;
        if (all && isMyParticipationSort(sort)) {
            result = requestRepository.findAllPublishedWithMyParticipation(
                    normalizedType, regionId, normalizedKeyword, currentUserId,
                    PageRequest.of(pageNumber - 1, size));
        } else {
            result = requestRepository.findAll(spec, PageRequest.of(pageNumber - 1, size, parseSort(sort)));
        }
        return PageResult.from(result, summarize(result.getContent(), currentUserId));
    }

    @Transactional(readOnly = true)
    public RequestDetailVO detail(Long requestId, Long currentUserId) {
        HelpRequest request = requireRequest(requestId);
        List<HelpRequestFile> files = fileRepository.findOrderedByRequestId(requestId);
        ResponseCounts counts = loadResponseCounts(List.of(requestId)).getOrDefault(requestId, new ResponseCounts());
        HelpType type = typeRepository.findById(request.getTypeCode()).orElse(null);
        Region region = regionRepository.findById(request.getRegionId()).orElse(null);
        User publisher = requireUser(request.getPublishUserId());

        boolean mine = request.getPublishUserId().equals(currentUserId);
        boolean modifiable = mine && request.getStatus() == HelpRequest.STATUS_PUBLISHED && counts.total == 0;

        List<AttachmentVO> attachments = files.stream().map(AttachmentVO::from).toList();
        FileSummary summary = summarizeFiles(files);

        List<HelpResponse> myResponses = myActiveResponses(currentUserId, requestId);
        Long myResponseId = myResponses.stream()
                .filter(response -> response.getStatus() == HelpResponse.STATUS_PENDING)
                .map(HelpResponse::getResponseId)
                .findFirst()
                .orElse(myResponses.isEmpty() ? null : myResponses.get(0).getResponseId());

        return new RequestDetailVO(
                request.getRequestId(),
                request.getTitle(),
                request.getTypeCode(),
                type == null ? request.getTypeCode() : type.getTypeName(),
                request.getRegionId(),
                region == null ? null : region.getRegionName(),
                region == null ? null : RegionVO.displayNameOf(region),
                request.getStatus(),
                HelpRequest.statusName(request.getStatus()),
                request.getDescription(),
                summary.coverUrl,
                summary.imageCount,
                summary.videoCount,
                (int) counts.total,
                (int) counts.pending,
                (int) counts.accepted,
                myResponses.size(),
                myResponseId,
                modifiable,
                modifiable,
                mine,
                UserBriefVO.from(publisher),
                attachments,
                VoUtils.format(request.getCreateTime()),
                VoUtils.format(request.getUpdateTime()));
    }

    // ---------- 写操作 ----------

    @Transactional
    public RequestDetailVO create(Long currentUserId, SaveRequestRequest req) {
        validateSave(req);
        List<Long> attachmentIds = normalizeAttachmentIds(req.attachmentIds());
        validateAttachments(attachmentIds, currentUserId, null);

        LocalDateTime now = LocalDateTime.now();
        HelpRequest request = new HelpRequest();
        request.setPublishUserId(currentUserId);
        request.setRegionId(req.regionId());
        request.setTypeCode(req.typeCode().trim());
        request.setTitle(req.title().trim());
        request.setDescription(req.description().trim());
        request.setStatus(HelpRequest.STATUS_PUBLISHED);
        request.setCreateTime(now);
        request.setUpdateTime(now);
        request = requestRepository.save(request);

        bindFiles(attachmentIds, request.getRequestId());
        return detail(request.getRequestId(), currentUserId);
    }

    @Transactional
    public RequestDetailVO update(Long currentUserId, Long requestId, SaveRequestRequest req) {
        HelpRequest request = requireRequest(requestId);
        requireOwner(request, currentUserId);
        if (request.getStatus() == HelpRequest.STATUS_CANCELLED) {
            throw new BusinessException(ApiCode.REQUEST_CANCELLED);
        }
        if (responseRepository.existsByRequestId(requestId)) {
            throw new BusinessException(ApiCode.REQUEST_CANNOT_MODIFY);
        }

        validateSave(req);
        List<Long> attachmentIds = normalizeAttachmentIds(req.attachmentIds());
        validateAttachments(attachmentIds, currentUserId, requestId);

        Set<Long> keep = new HashSet<>(attachmentIds);
        for (HelpRequestFile file : fileRepository.findOrderedByRequestId(requestId)) {
            if (!keep.contains(file.getFileId())) {
                file.setRequestId(null);
                file.setSortNo(0);
                fileRepository.save(file);
            }
        }
        bindFiles(attachmentIds, requestId);

        request.setRegionId(req.regionId());
        request.setTypeCode(req.typeCode().trim());
        request.setTitle(req.title().trim());
        request.setDescription(req.description().trim());
        request.setUpdateTime(LocalDateTime.now());
        requestRepository.save(request);

        return detail(requestId, currentUserId);
    }

    @Transactional
    public void delete(Long currentUserId, Long requestId) {
        HelpRequest request = requireRequest(requestId);
        requireOwner(request, currentUserId);
        if (request.getStatus() == HelpRequest.STATUS_CANCELLED) {
            throw new BusinessException(ApiCode.REQUEST_CANCELLED);
        }
        if (responseRepository.existsByRequestId(requestId)) {
            throw new BusinessException(ApiCode.REQUEST_CANNOT_DELETE);
        }
        request.setStatus(HelpRequest.STATUS_CANCELLED);
        request.setUpdateTime(LocalDateTime.now());
        requestRepository.save(request);
    }

    // ---------- 响应 ----------

    @Transactional(readOnly = true)
    public PageResult<ResponseVO> responses(Long currentUserId, String userType, Long requestId,
                                            Integer status, Integer page, Integer pageSize) {
        HelpRequest request = requireRequest(requestId);
        boolean owner = request.getPublishUserId().equals(currentUserId);
        if (!owner && !USER_TYPE_ADMIN.equals(userType)) {
            throw new BusinessException(ApiCode.NOT_REQUEST_OWNER);
        }

        int pageNumber = page == null ? 1 : page;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (pageNumber < 1) {
            throw paramInvalid(Map.of("page", "页码不能小于 1"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw paramInvalid(Map.of("pageSize", "每页条数需在 1~50 之间"));
        }

        Pageable pageable = PageRequest.of(pageNumber - 1, size, Sort.by(Sort.Direction.ASC, "createTime"));
        Page<HelpResponse> result = status == null
                ? responseRepository.findByRequestId(requestId, pageable)
                : responseRepository.findByRequestIdAndStatus(requestId, status, pageable);

        Map<Long, User> responders = loadUsers(
                result.getContent().stream().map(HelpResponse::getResponderId).distinct().toList());
        Map<Long, List<HelpRequestFile>> filesByResponse = fileRepository.findOrderedByResponseIdIn(
                result.getContent().stream().map(HelpResponse::getResponseId).toList()).stream()
                .collect(Collectors.groupingBy(HelpRequestFile::getResponseId));
        boolean hasAccepted = successRepository.existsByRequestId(requestId);
        boolean requestPublished = request.getStatus() == HelpRequest.STATUS_PUBLISHED;

        List<ResponseVO> list = result.getContent().stream().map(response -> {
            User responder = responders.get(response.getResponderId());
            boolean canAccept = owner && requestPublished && response.getStatus() == HelpResponse.STATUS_PENDING && !hasAccepted;
            boolean canReject = owner && response.getStatus() == HelpResponse.STATUS_PENDING;
            return new ResponseVO(
                    response.getResponseId(),
                    response.getRequestId(),
                    responder == null ? null : UserBriefVO.from(responder),
                    response.getDescription(),
                    filesByResponse.getOrDefault(response.getResponseId(), List.of()).stream()
                            .map(AttachmentVO::from).toList(),
                    response.getStatus(),
                    HelpResponse.statusName(response.getStatus()),
                    canAccept,
                    canReject,
                    false,
                    false,
                    VoUtils.format(response.getCreateTime()),
                    VoUtils.format(response.getUpdateTime()));
        }).toList();

        return PageResult.from(result, list);
    }

    // ---------- 字典 ----------

    @Transactional(readOnly = true)
    public List<HelpTypeVO> types() {
        return typeRepository.findAllByOrderBySortNoAsc().stream().map(HelpTypeVO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RegionVO> regions(String province) {
        List<Region> regions = (province == null || province.isBlank())
                ? regionRepository.findAllByOrderBySortNoAsc()
                : regionRepository.findByProvinceContainingIgnoreCaseOrderBySortNoAsc(province.trim());
        return regions.stream().map(RegionVO::from).toList();
    }

    // ---------- 内部工具 ----------

    private static final class ResponseCounts {
        long total;
        long pending;
        long accepted;
    }

    private static final class FileSummary {
        String coverUrl;
        int imageCount;
        int videoCount;
    }

    private FileSummary summarizeFiles(List<HelpRequestFile> files) {
        FileSummary summary = new FileSummary();
        for (HelpRequestFile file : files) {
            if (file.getMediaType() == MediaType.IMAGE) {
                summary.imageCount++;
                if (summary.coverUrl == null) {
                    summary.coverUrl = AttachmentVO.from(file).url();
                }
            } else {
                summary.videoCount++;
            }
        }
        return summary;
    }

    public List<RequestSummaryVO> summarize(List<HelpRequest> requests, Long currentUserId) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> ids = requests.stream().map(HelpRequest::getRequestId).toList();

        Map<Long, List<HelpRequestFile>> filesByRequest = fileRepository.findOrderedByRequestIdIn(ids).stream()
                .collect(Collectors.groupingBy(HelpRequestFile::getRequestId));

        Map<Long, ResponseCounts> responseCounts = loadResponseCounts(ids);

        Map<Long, List<HelpResponse>> myResponsesByRequest = responseRepository
                .findByResponderIdAndRequestIdIn(currentUserId, ids).stream()
                .filter(response -> response.getStatus() != HelpResponse.STATUS_CANCELLED)
                .collect(Collectors.groupingBy(HelpResponse::getRequestId));

        Map<String, String> typeNames = typeRepository.findByTypeCodeIn(
                        requests.stream().map(HelpRequest::getTypeCode).distinct().toList()).stream()
                .collect(Collectors.toMap(HelpType::getTypeCode, HelpType::getTypeName));

        Map<Long, Region> regions = regionRepository.findByRegionIdIn(
                        requests.stream().map(HelpRequest::getRegionId).distinct().toList()).stream()
                .collect(Collectors.toMap(Region::getRegionId, Function.identity()));

        return requests.stream().map(request -> {
            FileSummary summary = summarizeFiles(filesByRequest.getOrDefault(request.getRequestId(), List.of()));
            ResponseCounts counts = responseCounts.getOrDefault(request.getRequestId(), new ResponseCounts());
            boolean modifiable = request.getPublishUserId().equals(currentUserId)
                    && request.getStatus() == HelpRequest.STATUS_PUBLISHED
                    && counts.total == 0;
            Region region = regions.get(request.getRegionId());

            return new RequestSummaryVO(
                    request.getRequestId(),
                    request.getTitle(),
                    request.getTypeCode(),
                    typeNames.getOrDefault(request.getTypeCode(), request.getTypeCode()),
                    request.getRegionId(),
                    region == null ? null : region.getRegionName(),
                    region == null ? null : RegionVO.displayNameOf(region),
                    request.getStatus(),
                    HelpRequest.statusName(request.getStatus()),
                    summary.coverUrl,
                    summary.imageCount,
                    summary.videoCount,
                    (int) counts.total,
                    (int) counts.pending,
                    (int) counts.accepted,
                    myResponsesByRequest.getOrDefault(request.getRequestId(), List.of()).size(),
                    modifiable,
                    modifiable,
                    VoUtils.format(request.getCreateTime()),
                    VoUtils.format(request.getUpdateTime()));
        }).toList();
    }

    private Map<Long, ResponseCounts> loadResponseCounts(List<Long> requestIds) {
        Map<Long, ResponseCounts> map = new HashMap<>();
        for (Object[] row : responseRepository.countGroupByRequestAndStatus(requestIds)) {
            Long requestId = (Long) row[0];
            int status = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            ResponseCounts counts = map.computeIfAbsent(requestId, key -> new ResponseCounts());
            counts.total += count;
            if (status == HelpResponse.STATUS_PENDING) {
                counts.pending += count;
            } else if (status == HelpResponse.STATUS_ACCEPTED) {
                counts.accepted += count;
            }
        }
        return map;
    }

    private Specification<HelpRequest> buildSpec(Long currentUserId, boolean all, Integer status,
                                                 String typeCode, Long regionId, String keyword) {
        Specification<HelpRequest> spec = (root, query, cb) -> cb.conjunction();
        if (all) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), HelpRequest.STATUS_PUBLISHED));
        } else {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("publishUserId"), currentUserId));
            if (status != null) {
                int statusValue = status;
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusValue));
            }
        }
        if (typeCode != null) {
            String code = typeCode;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("typeCode"), code));
        }
        if (regionId != null) {
            Long rid = regionId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("regionId"), rid));
        }
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("description"), like)));
        }
        return spec;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createTime");
        }
        String[] parts = sort.trim().split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";
        String property = switch (field) {
            case "createTime" -> "createTime";
            case "updateTime" -> "updateTime";
            default -> throw paramInvalid(Map.of("sort", "排序字段不合法"));
        };
        Sort.Direction dir = switch (direction) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw paramInvalid(Map.of("sort", "排序方向不合法"));
        };
        return Sort.by(dir, property);
    }

    private void validateSave(SaveRequestRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();

        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty()) {
            errors.put("title", "请输入需求主题");
        } else if (title.length() > TITLE_MAX_LENGTH) {
            errors.put("title", "需求主题不能超过 50 个字符");
        }

        String typeCode = req.typeCode() == null ? "" : req.typeCode().trim();
        if (typeCode.isEmpty()) {
            errors.put("typeCode", "请选择帮忙类型");
        } else if (!typeRepository.existsById(typeCode)) {
            errors.put("typeCode", "帮忙类型不存在");
        }

        if (req.regionId() == null) {
            errors.put("regionId", "请选择地域");
        } else if (!regionRepository.existsById(req.regionId())) {
            errors.put("regionId", "地域不存在");
        }

        String description = req.description() == null ? "" : req.description().trim();
        if (description.isEmpty()) {
            errors.put("description", "请输入需求描述");
        } else if (description.length() > DESCRIPTION_MAX_LENGTH) {
            errors.put("description", "需求描述不能超过 1000 个字符");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(ApiCode.PARAM_INVALID, ApiCode.PARAM_INVALID.getMessage(), errors);
        }
    }

    private List<Long> normalizeAttachmentIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                seen.add(id);
            }
        }
        return List.copyOf(seen);
    }

    private void validateAttachments(List<Long> ids, Long currentUserId, Long currentRequestId) {
        if (ids.isEmpty()) {
            return;
        }
        if (ids.size() > MAX_ATTACHMENTS) {
            throw new BusinessException(ApiCode.FILE_INVALID, "附件总数不能超过 7 个");
        }
        List<HelpRequestFile> files = fileRepository.findAllById(ids);
        if (files.size() != ids.size()) {
            throw new BusinessException(ApiCode.PARAM_INVALID, "部分附件不存在");
        }
        int imageCount = 0;
        int videoCount = 0;
        for (HelpRequestFile file : files) {
            if (!file.getUploaderId().equals(currentUserId)) {
                throw new BusinessException(ApiCode.PARAM_INVALID, "附件不属于当前用户");
            }
            if (file.getRequestId() != null && !file.getRequestId().equals(currentRequestId)) {
                throw new BusinessException(ApiCode.PARAM_INVALID, "附件已被其他需求引用");
            }
            if (file.getResponseId() != null) {
                throw new BusinessException(ApiCode.PARAM_INVALID, "附件已被响应引用");
            }
            if (file.getMediaType() == MediaType.IMAGE) {
                imageCount++;
            } else {
                videoCount++;
            }
        }
        if (imageCount > MAX_IMAGES) {
            throw new BusinessException(ApiCode.FILE_INVALID, "图片最多 6 张");
        }
        if (videoCount > MAX_VIDEOS) {
            throw new BusinessException(ApiCode.FILE_INVALID, "视频最多 1 个");
        }
    }

    private void bindFiles(List<Long> ids, Long requestId) {
        int sortNo = 1;
        for (Long id : ids) {
            HelpRequestFile file = fileRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiCode.FILE_NOT_FOUND));
            file.setRequestId(requestId);
            file.setSortNo(sortNo++);
            fileRepository.save(file);
        }
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    private HelpRequest requireRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ApiCode.REQUEST_NOT_FOUND));
    }

    private List<HelpResponse> myActiveResponses(Long currentUserId, Long requestId) {
        return responseRepository.findByResponderIdAndRequestIdIn(currentUserId, List.of(requestId)).stream()
                .filter(response -> response.getStatus() != HelpResponse.STATUS_CANCELLED)
                .toList();
    }

    private boolean isMyParticipationSort(String sort) {
        return sort != null && sort.trim().toLowerCase().startsWith("myparticipation");
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
    }

    private void requireOwner(HelpRequest request, Long currentUserId) {
        if (!request.getPublishUserId().equals(currentUserId)) {
            throw new BusinessException(ApiCode.NOT_REQUEST_OWNER);
        }
    }

    private BusinessException paramInvalid(Map<String, String> errors) {
        return new BusinessException(ApiCode.PARAM_INVALID, ApiCode.PARAM_INVALID.getMessage(), errors);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
