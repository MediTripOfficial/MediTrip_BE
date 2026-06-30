package com.meditrip.user.domain.repository;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.domain.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

}
