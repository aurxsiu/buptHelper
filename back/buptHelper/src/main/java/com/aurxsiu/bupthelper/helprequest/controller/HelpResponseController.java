package com.aurxsiu.bupthelper.helprequest.controller;

import com.aurxsiu.bupthelper.common.ApiResponse;
import com.aurxsiu.bupthelper.helprequest.dto.SubmitResponseRequest;
import com.aurxsiu.bupthelper.helprequest.service.HelpResponseService;
import com.aurxsiu.bupthelper.helprequest.vo.MyResponseVO;
import com.aurxsiu.bupthelper.helprequest.vo.PageResult;
import com.aurxsiu.bupthelper.helprequest.vo.ResponseDetailVO;
import com.aurxsiu.bupthelper.helprequest.vo.ResponseStatusVO;
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

/**
 * “我来也”响应接口，见 doc/接口设计-我来也模块.md 4.1 ~ 4.7。
 */
@RestController
@RequestMapping("/api/responses")
public class HelpResponseController {

    private final HelpResponseService responseService;

    public HelpResponseController(HelpResponseService responseService) {
        this.responseService = responseService;
    }

    @GetMapping
    public ApiResponse<PageResult<MyResponseVO>> listMine(
            @RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "sort", required = false) String sort) {
        return ApiResponse.ok(responseService.listMine(
                currentUser.userId(), status, page, pageSize, sort));
    }

    @GetMapping("/{responseId}")
    public ApiResponse<ResponseDetailVO> detail(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                                @PathVariable Long responseId) {
        return ApiResponse.ok(responseService.detail(currentUser.userId(), currentUser.userType(), responseId));
    }

    @PostMapping
    public ApiResponse<ResponseDetailVO> submit(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                                @RequestBody SubmitResponseRequest request) {
        return ApiResponse.ok("响应提交成功", responseService.submit(currentUser.userId(), request));
    }

    @PutMapping("/{responseId}")
    public ApiResponse<ResponseDetailVO> update(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                                @PathVariable Long responseId,
                                                @RequestBody SubmitResponseRequest request) {
        return ApiResponse.ok("修改成功", responseService.update(currentUser.userId(), responseId, request));
    }

    @DeleteMapping("/{responseId}")
    public ApiResponse<Void> cancel(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                    @PathVariable Long responseId) {
        responseService.cancel(currentUser.userId(), responseId);
        return ApiResponse.ok("响应已取消", null);
    }

    @PutMapping("/{responseId}/accept")
    public ApiResponse<ResponseStatusVO> accept(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                                @PathVariable Long responseId) {
        return ApiResponse.ok("已接受该响应", responseService.accept(currentUser.userId(), responseId));
    }

    @PutMapping("/{responseId}/reject")
    public ApiResponse<ResponseStatusVO> reject(@RequestAttribute(AuthInterceptor.ATTR_USER) AuthUser currentUser,
                                                @PathVariable Long responseId) {
        return ApiResponse.ok("已拒绝该响应", responseService.reject(currentUser.userId(), responseId));
    }
}
