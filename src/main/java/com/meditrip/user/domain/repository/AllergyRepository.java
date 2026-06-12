package com.meditrip.user.domain.repository;

import com.meditrip.user.domain.entity.Allergy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {
    List<Allergy> findAllByNameIn(List<String> allergies);
}
