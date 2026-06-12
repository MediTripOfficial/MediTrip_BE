package com.meditrip.user.application;

import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.UserStatus;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findLoginUserByEmail(String method, String email) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);

        return userRepository.findByEmailAndStatusIn(email,
                        List.of(UserStatus.ACTIVE, UserStatus.GUEST))
                .orElseThrow(() -> {
                    log.info("{} 유저 정보가 존재하지 않습니다. Email : [{}]", method, maskedEmail);
                    return new BadCredentialsException("Incorrect email or password.");
                });
    }

    @Transactional(readOnly = true)
    public void checkEmailDuplication(String email) {
        userRepository.findByEmailAndStatusIn(email, List.of(UserStatus.ACTIVE, UserStatus.GUEST))
                .ifPresent(u -> {
                    throw new DuplicateKeyException("Email already exists.");
                });
    }

}
