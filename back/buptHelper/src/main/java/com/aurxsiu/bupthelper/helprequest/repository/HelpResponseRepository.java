package com.aurxsiu.bupthelper.helprequest.repository;

import com.aurxsiu.bupthelper.helprequest.entity.HelpResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HelpResponseRepository extends JpaRepository<HelpResponse, Long> {

    boolean existsByRequestId(Long requestId);

    Page<HelpResponse> findByRequestId(Long requestId, Pageable pageable);

    Page<HelpResponse> findByRequestIdAndStatus(Long requestId, int status, Pageable pageable);

    Page<HelpResponse> findByResponderId(Long responderId, Pageable pageable);

    Page<HelpResponse> findByResponderIdAndStatus(Long responderId, int status, Pageable pageable);

    List<HelpResponse> findByRequestIdAndStatus(Long requestId, int status);

    List<HelpResponse> findByResponderIdAndRequestIdIn(Long responderId, Collection<Long> requestIds);

    boolean existsByResponderIdAndRequestIdAndStatus(Long responderId, Long requestId, int status);

    /** 按需求、状态聚合响应数，返回 [requestId, status, count] */
    @Query("select r.requestId, r.status, count(r) from HelpResponse r "
            + "where r.requestId in :requestIds group by r.requestId, r.status")
    List<Object[]> countGroupByRequestAndStatus(@Param("requestIds") Collection<Long> requestIds);
}
