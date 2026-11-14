package com.aurxsiu.bupthelper.helprequest.service;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import com.aurxsiu.bupthelper.helprequest.dto.SubmitResponseRequest;
import com.aurxsiu.bupthelper.helprequest.entity.HelpRequest;
import com.aurxsiu.bupthelper.helprequest.entity.HelpRequestFile;
import com.aurxsiu.bupthelper.helprequest.entity.HelpResponse;
import com.aurxsiu.bupthelper.helprequest.entity.HelpSuccess;
import com.aurxsiu.bupthelper.helprequest.entity.MediaType;
import com.aurxsiu.bupthelper.helprequest.repository.HelpRequestFileRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpRequestRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpResponseRepository;
import com.aurxsiu.bupthelper.helprequest.repository.HelpSuccessRepository;
import com.aurxsiu.bupthelper.helprequest.vo.AttachmentVO;
import com.aurxsiu.bupthelper.helprequest.vo.MyResponseVO;
import com.aurxsiu.bupthelper.helprequest.vo.PageResult;
import com.aurxsiu.bupthelper.helprequest.vo.RequestSummaryVO;
import com.aurxsiu.bupthelper.helprequest.vo.ResponseDetailVO;
import com.aurxsiu.bupthelper.helprequest.vo.ResponseStatusVO;
import com.aurxsiu.bupthelper.helprequest.vo.UserBriefVO;
import com.aurxsiu.bupthelper.helprequest.vo.VoUtils;
import com.aurxsiu.bupthelper.user.entity.User;
import com.aurxsiu.bupthelper.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * “我来也”响应业务：提交、查询我的响应、详情、修改、取消（删除）、接受 / 拒绝。
 * 规则见 doc/接口设计-我来也模块.md 第 4、6 章。
 */
@Service
public class HelpResponseService {

    private static final String USER_TYPE_ADMIN = "ADMIN";

    private static final int DESCRIPTION_MAX_LENGTH = 1000;
    private static final int MAX_ATTACHMENTS = 7;
    private static final int MAX_IMAGES = 6;
    private static final int MAX_VIDEOS = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final HelpRequestRepository requestRepository;
    private final HelpRequestFileRepository fileRepository;
    private final HelpResponseRepository responseRepository;
    private final HelpSuccessRepository successRepository;
    private final UserRepository userRepository;
    private final HelpRequestService requestService;

    public HelpResponseService(HelpRequestRepository requestRepository,
                               HelpRequestFileRepository fileRepository,
                               HelpResponseRepository responseRepository,
                               HelpSuccessRepository successRepository,
                               UserRepository userRepository,
                               HelpRequestService requestService) {
        this.requestRepository = requestRepository;
        this.fileRepository = fileRepository;
        this.responseRepository = responseRepository;
        this.successRepository = successRepository;
        this.userRepository = userRepository;
        this.requestService = requestService;
    }

    // ---------- 响应者侧：提交 / 查询 / 修改 / 取消 ----------

    @Transactional
    public ResponseDetailVO submit(Long currentUserId, SubmitResponseRequest req) {
        if (req.requestId() == null) {
            throw paramInvalid(Map.of("requestId", "请选择要响应的需求"));
        }
        validateDescription(req.description());

        HelpRequest request = requireRequest(req.requestId());
        if (request.getPublishUserId().equals(currentUserId)) {
            throw new BusinessException(ApiCode.RESPOND_OWN_REQUEST);
        }
        if (request.getStatus() == HelpRequest.STATUS_CANCELLED) {
            throw new BusinessException(ApiCode.REQUEST_CANCELLED);
        }
        if (successRepository.existsByRequestId(request.getRequestId())) {
            throw new BusinessException(ApiCode.REQUEST_ALREADY_HELPED);
        }
        if (responseRepository.existsByResponderIdAndRequestIdAndStatus(currentUserId, request.getRequestId(), HelpResponse.STATUS_PENDING)) {
            throw new BusinessException(ApiCode.RESPONSE_EXISTS);
        }

        List<Long> attachmentIds = normalizeAttachmentIds(req.attachmentIds());
        validateAttachments(attachmentIds, currentUserId, null);

        LocalDateTime now = LocalDateTime.now();
        HelpResponse response = new HelpResponse();
        response.setRequestId(request.getRequestId());
        response.setResponderId(currentUserId);
        response.setDescription(req.description().trim());
        response.setStatus(HelpResponse.STATUS_PENDING);
        response.setCreateTime(now);
        response.setUpdateTime(now);
        response = responseRepository.save(response);

        bindFiles(attachmentIds, response.getResponseId());
        return detail(currentUserId, null, response.getResponseId());
    }

    @Transactional(readOnly = true)
    public PageResult<MyResponseVO> listMine(Long currentUserId, Integer status, Integer page, Integer pageSize, String sort) {
        if (status != null && (status < HelpResponse.STATUS_PENDING || status > HelpResponse.STATUS_CANCELLED)) {
            throw paramInvalid(Map.of("status", "状态参数不合法"));
        }
        int pageNumber = page == null ? 1 : page;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (pageNumber < 1) {
            throw paramInvalid(Map.of("page", "页码不能小于 1"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw paramInvalid(Map.of("pageSize", "每页条数需在 1~50 之间"));
        }

        Pageable pageable = PageRequest.of(pageNumber - 1, size, parseSort(sort));
        Page<HelpResponse> result = status == null
                ? responseRepository.findByResponderId(currentUserId, pageable)
                : responseRepository.findByResponderIdAndStatus(currentUserId, status, pageable);

        List<HelpResponse> responses = result.getContent();
        List<Long> requestIds = responses.stream().map(HelpResponse::getRequestId).distinct().toList();
        Map<Long, RequestSummaryVO> requestSummaries = requestService.summarize(
                requestRepository.findAllById(requestIds), currentUserId).stream()
                .collect(Collectors.toMap(RequestSummaryVO::requestId, Function.identity()));

        Map<Long, List<HelpRequestFile>> filesByResponse = fileRepository.findOrderedByResponseIdIn(
                        responses.stream().map(HelpResponse::getResponseId).toList()).stream()
                .collect(Collectors.groupingBy(HelpRequestFile::getResponseId));

        List<MyResponseVO> list = responses.stream().map(response -> {
            boolean editable = response.getStatus() == HelpResponse.STATUS_PENDING;
            List<AttachmentVO> attachments = filesByResponse.getOrDefault(response.getResponseId(), List.of()).stream()
                    .map(AttachmentVO::from).toList();
            return new MyResponseVO(
                    response.getResponseId(),
                    response.getRequestId(),
                    response.getStatus(),
                    HelpResponse.statusName(response.getStatus()),
                    response.getDescription(),
                    attachments,
                    editable,
                    editable,
                    VoUtils.format(response.getCreateTime()),
                    VoUtils.format(response.getUpdateTime()),
                    requestSummaries.get(response.getRequestId()));
        }).toList();

        return PageResult.from(result, list);
    }

    @Transactional(readOnly = true)
    public ResponseDetailVO detail(Long currentUserId, String userType, Long responseId) {
        HelpResponse response = requireResponse(responseId);
        HelpRequest request = requireRequest(response.getRequestId());

        boolean responder = response.getResponderId().equals(currentUserId);
        boolean requestOwner = request.getPublishUserId().equals(currentUserId);
        if (!responder && !requestOwner && !USER_TYPE_ADMIN.equals(userType)) {
            throw new BusinessException(ApiCode.NOT_RESPONSE_OWNER);
        }

        User responderUser = requireUser(response.getResponderId());
        List<AttachmentVO> attachments = fileRepository.findOrderedByResponseId(responseId).stream()
                .map(AttachmentVO::from).toList();

        boolean pending = response.getStatus() == HelpResponse.STATUS_PENDING;
        boolean requestPublished = request.getStatus() == HelpRequest.STATUS_PUBLISHED;
        boolean hasAccepted = successRepository.existsByRequestId(request.getRequestId());

        boolean canModify = responder && pending;
        boolean canDelete = responder && pending;
        boolean canAccept = requestOwner && requestPublished && pending && !hasAccepted;
        boolean canReject = requestOwner && pending;

        RequestSummaryVO requestSummary = requestService.summarize(List.of(request), currentUserId).get(0);

        return new ResponseDetailVO(
                response.getResponseId(),
                response.getRequestId(),
                response.getStatus(),
                HelpResponse.statusName(response.getStatus()),
                response.getDescription(),
                attachments,
                canAccept,
                canReject,
                canModify,
                canDelete,
                VoUtils.format(response.getCreateTime()),
                VoUtils.format(response.getUpdateTime()),
                UserBriefVO.from(responderUser),
                requestSummary);
    }

    @Transactional
    public ResponseDetailVO update(Long currentUserId, Long responseId, SubmitResponseRequest req) {
        HelpResponse response = requireResponse(responseId);
        requireResponder(response, currentUserId);
        if (response.getStatus() != HelpResponse.STATUS_PENDING) {
            throw new BusinessException(ApiCode.RESPONSE_STATUS_INVALID);
        }
        validateDescription(req.description());

        List<Long> attachmentIds = normalizeAttachmentIds(req.attachmentIds());
        validateAttachments(attachmentIds, currentUserId, responseId);

        Set<Long> keep = new HashSet<>(attachmentIds);
        for (HelpRequestFile file : fileRepository.findOrderedByResponseId(responseId)) {
            if (!keep.contains(file.getFileId())) {
                file.setResponseId(null);
                file.setSortNo(0);
                fileRepository.save(file);
            }
        }
        bindFiles(attachmentIds, responseId);

        response.setDescription(req.description().trim());
        response.setUpdateTime(LocalDateTime.now());
        responseRepository.save(response);

        return detail(currentUserId, null, responseId);
    }

    @Transactional
    public void cancel(Long currentUserId, Long responseId) {
        HelpResponse response = requireResponse(responseId);
        requireResponder(response, currentUserId);
        if (response.getStatus() != HelpResponse.STATUS_PENDING) {
            throw new BusinessException(ApiCode.RESPONSE_STATUS_INVALID);
        }
        response.setStatus(HelpResponse.STATUS_CANCELLED);
        response.setUpdateTime(LocalDateTime.now());
        responseRepository.save(response);
    }

    // ---------- 需求发布者侧：接受 / 拒绝 ----------

    @Transactional
    public ResponseStatusVO accept(Long currentUserId, Long responseId) {
        HelpResponse response = requireResponse(responseId);
        HelpRequest request = requireRequest(response.getRequestId());
        requireOwner(request, currentUserId);

        if (request.getStatus() == HelpRequest.STATUS_CANCELLED) {
            throw new BusinessException(ApiCode.REQUEST_CANCELLED);
        }
        if (response.getStatus() != HelpResponse.STATUS_PENDING) {
            throw new BusinessException(ApiCode.RESPONSE_STATUS_INVALID);
        }
        if (successRepository.existsByRequestId(request.getRequestId())) {
            throw new BusinessException(ApiCode.REQUEST_ALREADY_ACCEPTED);
        }

        LocalDateTime now = LocalDateTime.now();
        List<HelpResponse> pending = responseRepository.findByRequestIdAndStatus(request.getRequestId(), HelpResponse.STATUS_PENDING);
        for (HelpResponse other : pending) {
            if (!other.getResponseId().equals(responseId)) {
                other.setStatus(HelpResponse.STATUS_REJECTED);
                other.setUpdateTime(now);
            }
        }
        responseRepository.saveAll(pending);

        response.setStatus(HelpResponse.STATUS_ACCEPTED);
        response.setUpdateTime(now);
        responseRepository.save(response);

        HelpSuccess success = new HelpSuccess();
        success.setRequestId(request.getRequestId());
        success.setPublishUserId(request.getPublishUserId());
        success.setResponseId(responseId);
        success.setResponderUserId(response.getResponderId());
        success.setAcceptTime(now);
        successRepository.save(success);

        return new ResponseStatusVO(responseId, HelpResponse.STATUS_ACCEPTED, HelpResponse.statusName(HelpResponse.STATUS_ACCEPTED));
    }

    @Transactional
    public ResponseStatusVO reject(Long currentUserId, Long responseId) {
        HelpResponse response = requireResponse(responseId);
        HelpRequest request = requireRequest(response.getRequestId());
        requireOwner(request, currentUserId);

        if (response.getStatus() != HelpResponse.STATUS_PENDING) {
            throw new BusinessException(ApiCode.RESPONSE_STATUS_INVALID);
        }
        response.setStatus(HelpResponse.STATUS_REJECTED);
        response.setUpdateTime(LocalDateTime.now());
        responseRepository.save(response);

        return new ResponseStatusVO(responseId, HelpResponse.STATUS_REJECTED, HelpResponse.statusName(HelpResponse.STATUS_REJECTED));
    }

    // ---------- 内部工具 ----------

    private void validateDescription(String description) {
        String value = description == null ? "" : description.trim();
        if (value.isEmpty()) {
            throw paramInvalid(Map.of("description", "请输入响应描述"));
        }
        if (value.length() > DESCRIPTION_MAX_LENGTH) {
            throw paramInvalid(Map.of("description", "响应描述不能超过 1000 个字符"));
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

    private void validateAttachments(List<Long> ids, Long currentUserId, Long currentResponseId) {
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
            if (file.getRequestId() != null) {
                throw new BusinessException(ApiCode.PARAM_INVALID, "附件已被需求引用");
            }
            if (file.getResponseId() != null && !file.getResponseId().equals(currentResponseId)) {
                throw new BusinessException(ApiCode.PARAM_INVALID, "附件已被其他响应引用");
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

    private void bindFiles(List<Long> ids, Long responseId) {
        int sortNo = 1;
        for (Long id : ids) {
            HelpRequestFile file = fileRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ApiCode.FILE_NOT_FOUND));
            file.setResponseId(responseId);
            file.setSortNo(sortNo++);
            fileRepository.save(file);
        }
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

    private HelpRequest requireRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ApiCode.REQUEST_NOT_FOUND));
    }

    private HelpResponse requireResponse(Long responseId) {
        return responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(ApiCode.RESPONSE_NOT_FOUND));
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

    private void requireResponder(HelpResponse response, Long currentUserId) {
        if (!response.getResponderId().equals(currentUserId)) {
            throw new BusinessException(ApiCode.NOT_RESPONSE_OWNER);
        }
    }

    private BusinessException paramInvalid(Map<String, String> errors) {
        return new BusinessException(ApiCode.PARAM_INVALID, ApiCode.PARAM_INVALID.getMessage(), errors);
    }
}
