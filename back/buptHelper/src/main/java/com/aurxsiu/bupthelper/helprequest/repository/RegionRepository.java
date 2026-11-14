package com.aurxsiu.bupthelper.helprequest.repository;

import com.aurxsiu.bupthelper.helprequest.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderBySortNoAsc();

    List<Region> findByRegionIdIn(Collection<Long> regionIds);

    List<Region> findByProvinceContainingIgnoreCaseOrderBySortNoAsc(String province);
}
