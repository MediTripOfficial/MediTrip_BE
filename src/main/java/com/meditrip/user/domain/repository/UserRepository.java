package com.meditrip.user.domain.repository;

import com.meditrip.common.domain.UserRole;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.domain.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmailAndStatusIn(String email, List<UserStatus> statuses);

    Optional<User> findByNicknameAndStatusIn(String nickname, List<UserStatus> statuses);

    Optional<User> findByIdAndStatusIn(UUID id, List<UserStatus> statuses);

    List<User> findByIdIn(List<UUID> ids);

    Page<User> findByCreatedAtBetween(Instant startInstant, Instant endInstant, Pageable pageable);

    Page<User> findByCreatedAtBetweenAndUserRoleNot(Instant createdAtAfter, Instant createdAtBefore, UserRole userRole,
                                                    Pageable pageable);

    Page<User> findByCountryAndCreatedAtBetweenAndUserRoleNot(String country, Instant startInstant, Instant endInstant,
                                                              UserRole userRole, Pageable pageable);

}
