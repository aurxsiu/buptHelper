package com.aurxsiu.bupthelper.helprequest.repository;

import com.aurxsiu.bupthelper.helprequest.entity.HelpRequestFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HelpRequestFileRepository extends JpaRepository<HelpRequestFile, Long> {

    @Query("select f from HelpRequestFile f where f.requestId = :requestId order by f.sortNo asc")
    List<HelpRequestFile> findOrderedByRequestId(@Param("requestId") Long requestId);

    @Query("select f from HelpRequestFile f where f.responseId = :responseId order by f.sortNo asc")
    List<HelpRequestFile> findOrderedByResponseId(@Param("responseId") Long responseId);

    @Query("select f from HelpRequestFile f where f.responseId in :responseIds order by f.responseId asc, f.sortNo asc")
    List<HelpRequestFile> findOrderedByResponseIdIn(@Param("responseIds") Collection<Long> responseIds);

    @Query("select f from HelpRequestFile f where f.requestId in :requestIds order by f.requestId asc, f.sortNo asc")
    List<HelpRequestFile> findOrderedByRequestIdIn(@Param("requestIds") Collection<Long> requestIds);
}
