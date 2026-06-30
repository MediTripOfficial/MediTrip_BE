package com.meditrip.user.application;

import com.meditrip.common.exception.NotFoundException;
import com.meditrip.common.util.SecurityUtils;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.UserStatus;
import com.meditrip.user.domain.exception.UserNotFoundException;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final UserConditionRepository userConditionRepository;
    private final UserAllergyRepository userAllergyRepository;

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

    @Transactional(readOnly = true)
    public void checkNicknameDuplication(String nickname) {
        userRepository.findByNicknameAndStatusIn(nickname, List.of(UserStatus.ACTIVE, UserStatus.GUEST))
                .ifPresent(u -> {
                    throw new DuplicateKeyException("Nickname already exists.");
                });
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(UUID userId) {
        User user = findById(userId, "유저 정보 조회");
        user.validateStatus();

        List<String> conditions = userConditionRepository.findConditionNamesByUserId(userId);
        List<String> allergies = userAllergyRepository.findAllergyNamesByUserId(userId);

        return UserInfoResponse.from(user, conditions, allergies);
    }

    @Transactional(readOnly = true)
    public User findById(UUID userId, String method) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.info("{} 유저 정보가 존재하지 않습니다. User Id : [{}]", method, userId);
                    return new NotFoundException("User Not Found");
                });
    }

    @Transactional
    public void deleteAllergyAndConditions(UUID userId) {
        userConditionRepository.deleteAllByUserId(userId);
        userAllergyRepository.deleteAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public void validUpdateUser(String nickname, UUID userId) {
        userRepository.findByNickname(nickname).ifPresent(user -> {
            if (user.getId().equals(userId)) {
                return;
            }

            UserStatus status = user.getStatus();

            if (status != UserStatus.DELETED && status != UserStatus.WITHDRAWN) {
                throw new DuplicateKeyException("Nickname already exists.");
            }
        });
    }

    @Transactional(readOnly = true)
    public User findValidUserByEmail(String method, String email) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);

        return userRepository.findByEmailAndStatusIn(email,
                        List.of(UserStatus.ACTIVE, UserStatus.GUEST))
                .orElseThrow(() -> {
                    log.info("{} 유저 정보가 존재하지 않습니다. Email : [{}]", method, maskedEmail);
                    return new UserNotFoundException();
                });
    }

    @Transactional(readOnly = true)
    public UserInfo getReviewUserInfo(UUID userId) {
        User user = userRepository.findByIdAndStatusIn(userId, List.of(UserStatus.ACTIVE))
                .orElseThrow(UserNotFoundException::new);

        return UserInfo.builder()
                .userId(user.getId())
                .gender(user.getGender().getEng())
                .country(user.getCountry())
                .weight(user.getWeight())
                .height(user.getHeight())
                .age(Period.between(user.getBirth(), LocalDate.now()).getYears())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> getNicknamesByUserIds(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }

}
