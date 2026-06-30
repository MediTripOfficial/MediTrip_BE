package com.meditrip.user.application;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.application.dto.request.SignupApplicationRequest;
import com.meditrip.user.domain.entity.Allergy;
import com.meditrip.user.domain.entity.Condition;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.UserAllergy;
import com.meditrip.user.domain.entity.UserCondition;
import com.meditrip.user.domain.repository.AllergyRepository;
import com.meditrip.user.domain.repository.ConditionRepository;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AllergyRepository allergyRepository;
    private final ConditionRepository conditionRepository;
    private final UserConditionRepository userConditionRepository;
    private final UserAllergyRepository userAllergyRepository;

    @Transactional
    public UUID signup(SignupApplicationRequest request) {
        UUID userId = UUID.randomUUID();
        log.info("회원가입을 진행합니다. id : {}", userId);

        validSignUp(request);

        String encodePassword = passwordEncoder.encode(request.getPassword());

        User user = User.createLocalUser(userId, request.getEmail(), encodePassword, request.getPassword(),
                request.getName(), request.getNickname(), request.getWeight(), request.getHeight(), request.getBirth(),
                request.getGender(), request.getCountry(), request.isMarketingTermsAgreed(), request.getProfileImg());

        User saved = userRepository.save(user);

        saveConditions(request.getUnderlyingDisease(), userId);
        saveAllergies(request.getAllergies(), userId);

        return saved.getId();
    }

    private void validSignUp(SignupApplicationRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            UserStatus status = user.getStatus();

            if (status != UserStatus.DELETED && status != UserStatus.WITHDRAWN) {
                log.info("이미 가입된 이메일로 회원가입 요청. email : {}, userStatus : {}", request.getEmail(), status);
                throw new DuplicateKeyException("Email already exists.");
            }
        });

        userRepository.findByNickname(request.getNickname()).ifPresent(user -> {
            UserStatus status = user.getStatus();

            if (status != UserStatus.DELETED && status != UserStatus.WITHDRAWN) {
                throw new DuplicateKeyException("Nickname already exists.");
            }
        });
    }

    protected void saveConditions(List<String> underlyingDisease, UUID userId) {
        if (underlyingDisease == null || underlyingDisease.isEmpty()) {
            return;
        }

        underlyingDisease = underlyingDisease.stream().map(String::toLowerCase).toList();

        List<Condition> existingConditions = conditionRepository.findAllByNameIn(underlyingDisease);
        Set<String> existingNames = existingConditions.stream()
                .map(Condition::getName)
                .collect(Collectors.toSet());

        List<Condition> newConditions = underlyingDisease.stream()
                .filter(name -> !existingNames.contains(name))
                .map(Condition::create)
                .toList();

        List<Condition> savedNewConditions = List.of();
        if (!newConditions.isEmpty()) {
            savedNewConditions = conditionRepository.saveAll(newConditions);
        }

        List<Condition> allConditions = new ArrayList<>(existingConditions);
        allConditions.addAll(savedNewConditions);

        List<UserCondition> userConditions = allConditions.stream()
                .map(condition -> UserCondition.builder()
                        .userId(userId)
                        .conditionId(condition.getId())
                        .build())
                .toList();

        userConditionRepository.saveAll(userConditions);
    }

    protected void saveAllergies(List<String> allergies, UUID userId) {
        if (allergies == null || allergies.isEmpty()) {
            return;
        }

        allergies = allergies.stream().map(String::toLowerCase).toList();

        List<Allergy> existingAllergies = allergyRepository.findAllByNameIn(allergies);
        Set<String> existingNames = existingAllergies.stream()
                .map(Allergy::getName)
                .collect(Collectors.toSet());

        List<Allergy> newAllergies = allergies.stream()
                .filter(name -> !existingNames.contains(name))
                .map(Allergy::create)
                .toList();

        List<Allergy> savedNewAllergies = List.of();
        if (!newAllergies.isEmpty()) {
            savedNewAllergies = allergyRepository.saveAll(newAllergies);
        }

        List<Allergy> allAllergies = new ArrayList<>(existingAllergies);
        allAllergies.addAll(savedNewAllergies);

        List<UserAllergy> userAllergy = allAllergies.stream()
                .map(allergy -> UserAllergy.builder()
                        .userId(userId)
                        .allergyId(allergy.getId())
                        .build())
                .toList();

        userAllergyRepository.saveAll(userAllergy);
    }

    public void verifyPasswordForLogin(String inputPassword, String storedPassword) {
        if (!passwordEncoder.matches(inputPassword, storedPassword)) {
            throw new BadCredentialsException("Incorrect email or password.");
        }
    }

    public void verifyPassword(String inputPassword, String storedPassword) {
        if (!passwordEncoder.matches(inputPassword, storedPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    @Transactional
    public void updatePassword(User user, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(newPassword, encodedPassword);
    }

}
