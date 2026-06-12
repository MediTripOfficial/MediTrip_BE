package com.meditrip.user.domain.repository;

import com.meditrip.user.domain.entity.UserAllergy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {

    @Query("""
                SELECT a.name FROM Allergy a
                JOIN UserAllergy ua ON a.id = ua.allergyId
                WHERE ua.userId = :userId
            """)
    List<String> findAllergyNamesByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

}
