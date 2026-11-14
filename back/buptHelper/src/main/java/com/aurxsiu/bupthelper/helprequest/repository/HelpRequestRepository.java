package com.aurxsiu.bupthelper.helprequest.repository;

import com.aurxsiu.bupthelper.helprequest.entity.HelpRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HelpRequestRepository extends JpaRepository<HelpRequest, Long>, JpaSpecificationExecutor<HelpRequest> {

    /**
     * 查询全部已发布需求，按“当前用户是否参与响应”优先、再按创建时间倒序排序，
     * 供“我来也”模块默认列表使用（sort=myParticipation）。
     */
    @Query("""
            select r from HelpRequest r
            where r.status = 0
              and (:typeCode is null or r.typeCode = :typeCode)
              and (:regionId is null or r.regionId = :regionId)
              and (:keyword is null or :keyword = '' or r.title like concat('%', :keyword, '%') or r.description like concat('%', :keyword, '%'))
            order by
              case when exists (
                  select 1 from HelpResponse resp
                  where resp.requestId = r.requestId and resp.responderId = :userId and resp.status <> 3
              ) then 0 else 1 end asc,
              r.createTime desc
            """)
    Page<HelpRequest> findAllPublishedWithMyParticipation(
            @Param("typeCode") String typeCode,
            @Param("regionId") Long regionId,
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            Pageable pageable);
}
