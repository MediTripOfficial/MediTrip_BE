package com.meditrip.user.domain.repository;

import com.meditrip.user.domain.entity.UserCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {
}
