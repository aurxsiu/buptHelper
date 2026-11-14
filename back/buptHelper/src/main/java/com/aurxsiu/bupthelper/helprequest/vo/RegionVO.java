package com.aurxsiu.bupthelper.helprequest.vo;

import com.aurxsiu.bupthelper.helprequest.entity.Region;

/**
 * 地域，见 doc/接口设计-请帮忙模块.md 2.2。
 */
public record RegionVO(Long regionId, String regionName, String city, String province, String displayName) {

    public static RegionVO from(Region region) {
        return new RegionVO(
                region.getRegionId(),
                region.getRegionName(),
                region.getCity(),
                region.getProvince(),
                displayNameOf(region));
    }

    public static String displayNameOf(Region region) {
        java.util.List<String> parts = java.util.stream.Stream
                .of(region.getProvince(), region.getCity(), region.getRegionName())
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return String.join(" · ", parts);
    }
}
