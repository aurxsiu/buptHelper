package com.aurxsiu.bupthelper.helprequest.repository;

import com.aurxsiu.bupthelper.helprequest.entity.HelpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HelpTypeRepository extends JpaRepository<HelpType, String> {

    List<HelpType> findAllByOrderBySortNoAsc();

    List<HelpType> findByTypeCodeIn(Collection<String> typeCodes);
}
