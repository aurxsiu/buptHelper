package com.aurxsiu.bupthelper.helprequest.controller;

import com.aurxsiu.bupthelper.common.ApiResponse;
import com.aurxsiu.bupthelper.helprequest.service.HelpRequestService;
import com.aurxsiu.bupthelper.helprequest.vo.RegionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地域字典接口，见 doc/接口设计-请帮忙模块.md 4.2。
 */
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final HelpRequestService helpRequestService;

    public RegionController(HelpRequestService helpRequestService) {
        this.helpRequestService = helpRequestService;
    }

    @GetMapping
    public ApiResponse<List<RegionVO>> regions(@RequestParam(name = "province", required = false) String province) {
        return ApiResponse.ok(helpRequestService.regions(province));
    }
}
