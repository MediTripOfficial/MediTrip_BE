package com.meditrip.user.domain.repository;

import com.meditrip.user.domain.entity.Condition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findAllByNameIn(List<String> underlyingDisease);

}
