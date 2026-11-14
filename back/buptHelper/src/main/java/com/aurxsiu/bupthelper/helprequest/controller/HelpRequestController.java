package com.aurxsiu.bupthelper.helprequest.controller;

import com.aurxsiu.bupthelper.common.ApiResponse;
import com.aurxsiu.bupthelper.helprequest.dto.SaveRequestRequest;
import com.aurxsiu.bupthelper.helprequest.service.HelpRequestService;
import com.aurxsiu.bupthelper.helprequest.vo.HelpTypeVO;
import com.aurxsiu.bupthelper.helprequest.vo.PageResult;
import com.aurxsiu.bupthelper.helprequest.vo.RequestDetailVO;
import com.aurxsiu.bupthelper.helprequest.vo.RequestSummaryVO;
import com.aurxsiu.bupthelper.helprequest.vo.ResponseVO;
import com.aurxsiu.bupthelper.security.AuthInterceptor;
import com.aurxsiu.bupthelper.security.AuthUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * “请帮忙”需求接口，见 doc/接口设计-请帮忙模块.md 4.1 ~ 4.8。
 */
@RestController
@RequestMapping("/api/requests")
public class HelpRequestController {

    private final HelpRequestService helpRequestService;

    public HelpRequestController(HelpRequestService helpRequestService) {
        this.helpRequestService = helpRequestService;
    }

    @GetMapping
    public ApiResponse<PageResult<RequestSummaryVO>> list(
            @RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
            @RequestParam(name = "scope", defaultValue = "mine") String scope,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "typeCode", required = false) String typeCode,
            @RequestParam(name = "regionId", required = false) Long regionId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sort", required = false) String sort) {
        return ApiResponse.ok(helpRequestService.list(
                currentUser.userId(), scope, status, typeCode, regionId, keyword, page, pageSize, sort));
    }

    @GetMapping("/types")
    public ApiResponse<List<HelpTypeVO>> types() {
        return ApiResponse.ok(helpRequestService.types());
    }

    @GetMapping("/{requestId}")
    public ApiResponse<RequestDetailVO> detail(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                               @PathVariable Long requestId) {
        return ApiResponse.ok(helpRequestService.detail(requestId, currentUser.userId()));
    }

    @PostMapping
    public ApiResponse<RequestDetailVO> create(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                               @RequestBody SaveRequestRequest request) {
        return ApiResponse.ok("发布成功", helpRequestService.create(currentUser.userId(), request));
    }

    @PutMapping("/{requestId}")
    public ApiResponse<RequestDetailVO> update(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                               @PathVariable Long requestId,
                                               @RequestBody SaveRequestRequest request) {
        return ApiResponse.ok("修改成功", helpRequestService.update(currentUser.userId(), requestId, request));
    }

    @DeleteMapping("/{requestId}")
    public ApiResponse<Void> delete(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                    @PathVariable Long requestId) {
        helpRequestService.delete(currentUser.userId(), requestId);
        return ApiResponse.ok("需求已删除", null);
    }

    @GetMapping("/{requestId}/responses")
    public ApiResponse<PageResult<ResponseVO>> responses(
            @RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
            @PathVariable Long requestId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.ok(helpRequestService.responses(
                currentUser.userId(), currentUser.userType(), requestId, status, page, pageSize));
    }
}
