package com.aurxsiu.bupthelper.helprequest.vo;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 统一分页结构，见 doc/接口设计-请帮忙模块.md 1.4。
 */
public record PageResult<T>(
        List<T> list,
        int page,
        int pageSize,
        long total,
        int totalPages,
        boolean hasPrev,
        boolean hasNext) {

    public static <T> PageResult<T> from(Page<?> page, List<T> list) {
        return new PageResult<>(
                list,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasPrevious(),
                page.hasNext());
    }

    public static <T> PageResult<T> from(Page<T> page) {
        return from(page, page.getContent());
    }
}
