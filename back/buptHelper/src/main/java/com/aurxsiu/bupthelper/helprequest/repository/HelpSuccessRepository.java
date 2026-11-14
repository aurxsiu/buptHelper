package com.aurxsiu.bupthelper.helprequest.repository;

import com.aurxsiu.bupthelper.helprequest.entity.HelpSuccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpSuccessRepository extends JpaRepository<HelpSuccess, Long> {

    boolean existsByRequestId(Long requestId);
}
