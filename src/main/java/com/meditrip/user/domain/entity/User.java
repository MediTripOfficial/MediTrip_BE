package com.meditrip.user.domain.entity;

import com.meditrip.common.domain.BaseEntity;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.exception.NotFoundException;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"password", "email", "birth"})
@Getter
@Builder
@Slf4j
public class User extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    private String password;

    private String name;
    private String nickname;
    private Double weight;
    private Double height;
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String country;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private Boolean isMarketingTermsAgreed;
    private String profileImg;

    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$";

    public static User createLocalUser(UUID id, String email, String encodedPassword, String password, String name,
                                       String nickname,
                                       Double weight, Double height, LocalDate birth, Gender gender, String country,
                                       Boolean isMarketingTermsAgreed, String profileImg) {
        validateName(id, name);
        validatePassword(id, password);
        validateBirth(id, birth);
        validateGender(id, gender);

        return User.builder()
                .id(id)
                .email(email)
                .password(encodedPassword)
                .name(name)
                .nickname(nickname)
                .weight(weight)
                .height(height)
                .birth(birth)
                .gender(gender)
                .country(country)
                .isMarketingTermsAgreed(isMarketingTermsAgreed)
                .provider(Provider.LOCAL)
                .status(UserStatus.ACTIVE)
                .profileImg(profileImg)
                .build();
    }

    public static User oauthSignupUser(UUID userId, String email, String name, Provider provider) {
        return User.builder()
                .id(userId)
                .email(email)
                .name(name)
                .provider(provider)
                .status(UserStatus.GUEST)
                .isMarketingTermsAgreed(false)
                .build();
    }

    public void updateInfo(String name, String nickname, Double weight, Double height, LocalDate birth, Gender gender,
                           String country, Boolean isMarketingTermsAgreed, String profileImg) {
        validateName(this.id, name);
        validateBirth(this.id, birth);
        validateGender(this.id, gender);

        this.name = name;
        this.nickname = nickname;
        this.weight = weight;
        this.height = height;
        this.birth = birth;
        this.gender = gender;
        this.country = country;
        this.isMarketingTermsAgreed = isMarketingTermsAgreed;
        this.profileImg = profileImg;
    }

    public void onboarding(UUID id, String name, String nickname, Double weight, Double height, LocalDate birth,
                           Gender gender, String country, Boolean isMarketingTermsAgreed, String profileImg) {
        validateName(id, name);
        validateBirth(id, birth);
        validateGender(id, gender);

        this.name = name;
        this.nickname = nickname;
        this.weight = weight;
        this.height = height;
        this.birth = birth;
        this.gender = gender;
        this.country = country;
        this.isMarketingTermsAgreed = isMarketingTermsAgreed;
        this.status = UserStatus.ACTIVE;
        this.profileImg = profileImg;
    }

    private static void validateName(UUID id, String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("도메인 제약 위반: 이름이 비어있음. User Id : [{}]", id);
            throw new IllegalArgumentException("Name is required.");
        }
        if (name.length() < 2 || name.length() > 20) {
            log.warn("이름 글자수 제한 위반: [{}]. User Id : [{}]", name, id);
            throw new IllegalArgumentException("Name must be between 2 and 20 characters.");
        }
    }

    private static void validateBirth(UUID id, LocalDate birth) {
        if (birth == null) {
            log.warn("도메인 제약 위반: 생년월일이 누락됨. User Id : [{}]", id);
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (birth.isAfter(LocalDate.now())) {
            log.warn("생년월일이 미래 날짜입니다. 입력 날짜 : [{}], User Id : [{}]", birth, id);
            throw new IllegalArgumentException("Date of birth can't be in the future.");
        }
        if (birth.isBefore(LocalDate.now().minusYears(150))) {
            log.warn("생년월일이 비정상적인 과거 날짜입니다. 입력 날짜 : [{}], User Id : [{}]", birth, id);
            throw new IllegalArgumentException("Please enter a valid date of birth.");
        }
    }

    private static void validateGender(UUID id, Gender gender) {
        if (gender == null) {
            log.warn("도메인 제약 위반: 선택된 성별이 null입니다. User Id : [{}]", id);
            throw new IllegalArgumentException("Gender is required.");
        }
    }

    public void updatePassword(String plainPassword, String encodedPassword) {
        validatePassword(this.id, plainPassword);
        this.password = encodedPassword;
    }

    private static void validatePassword(UUID id, String password) {
        if (password == null || password.trim().isEmpty()) {
            log.warn("비밀번호가 비어있음. User Id : [{}]", id);
            throw new IllegalArgumentException("Password is required.");
        }
        if (!java.util.regex.Pattern.matches(PASSWORD_REGEX, password)) {
            log.warn("유효한 비밀번호 형식이 아닙니다. User Id : [{}]", id);
            throw new IllegalArgumentException(
                    "Password must be 8–20 characters and include a letter, number, and special character.");
        }
    }

    public void validateStatusForLogin() {
        switch (this.status) {
            case WITHDRAWN, DELETED -> throw new BadCredentialsException("Incorrect email or password.");
            default -> {
            }
        }
    }

    public void validateStatus() {
        switch (this.status) {
            case WITHDRAWN, DELETED -> throw new NotFoundException("User Not Found");
            default -> {
            }
        }
    }

    public void withdrawn() {
        this.status = UserStatus.WITHDRAWN;
    }

    public Integer getAge() {
        if (birth == null) {
            return null;
        }

        return Period.between(birth, LocalDate.now()).getYears();
    }

}
