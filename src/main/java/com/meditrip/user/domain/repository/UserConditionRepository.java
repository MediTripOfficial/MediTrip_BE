package com.meditrip.user.domain.repository;

import com.meditrip.user.domain.entity.UserCondition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

    @Query("""
            SELECT c.name FROM Condition c
            JOIN UserCondition uc ON c.id = uc.conditionId
            WHERE uc.userId = :userId
            """)
    List<String> findConditionNamesByUserId(UUID userId);

}
