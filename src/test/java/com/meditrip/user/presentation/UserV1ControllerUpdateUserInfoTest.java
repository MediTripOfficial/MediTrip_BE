package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.domain.entity.Allergy;
import com.meditrip.user.domain.entity.Condition;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.UserAllergy;
import com.meditrip.user.domain.entity.UserCondition;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.AllergyRepository;
import com.meditrip.user.domain.repository.ConditionRepository;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import com.meditrip.user.presentation.dto.request.UpdateUserInfoRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class UserV1ControllerUpdateUserInfoTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private UserConditionRepository userConditionRepository;

    @Autowired
    private UserAllergyRepository userAllergyRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @AfterEach
    void tearDown() {
        userConditionRepository.deleteAllInBatch();
        userAllergyRepository.deleteAllInBatch();
        conditionRepository.deleteAllInBatch();
        allergyRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("유저 정보 업데이트에 성공한다.")
    @Test
    void shouldSucceedUpdateUserInfo_whenValidRequestIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest();

        //when
        MvcResult mvcResult = mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

        assertThat(resultNode.get("name").asText()).isEqualTo(request.getName());
        assertThat(resultNode.get("nickname").asText()).isEqualTo(request.getNickname());
        assertThat(resultNode.get("country").asText()).isEqualTo(request.getCountry());
    }

    @DisplayName("업데이트 시 기존 conditions/allergies가 삭제되고 새 것으로 교체된다.")
    @Test
    void shouldReplaceConditionsAndAllergies_whenUpdating() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        Condition oldCondition = conditionRepository.save(Condition.create("diabetes"));
        Allergy oldAllergy = allergyRepository.save(Allergy.create("milk"));
        userConditionRepository.save(UserCondition.builder().userId(userId).conditionId(oldCondition.getId()).build());
        userAllergyRepository.save(UserAllergy.builder().userId(userId).allergyId(oldAllergy.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder()
                .underlyingDisease(List.of("asthma"))
                .allergies(List.of("eggs"))
                .build();

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        //then
        List<String> conditions = userConditionRepository.findConditionNamesByUserId(userId);
        List<String> allergies = userAllergyRepository.findAllergyNamesByUserId(userId);

        assertThat(conditions).containsExactly("asthma");
        assertThat(conditions).doesNotContain("diabetes");
        assertThat(allergies).containsExactly("eggs");
        assertThat(allergies).doesNotContain("milk");
    }

    @DisplayName("conditions, allergies가 null이면 기존 것이 삭제되고 빈 상태로 업데이트된다.")
    @Test
    void shouldDeleteConditionsAndAllergies_whenRequestHasNullLists() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        Condition condition = conditionRepository.save(Condition.create("diabetes"));
        userConditionRepository.save(UserCondition.builder().userId(userId).conditionId(condition.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder()
                .underlyingDisease(null)
                .allergies(null)
                .build();

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        //then
        assertThat(userConditionRepository.findConditionNamesByUserId(userId)).isEmpty();
        assertThat(userAllergyRepository.findAllergyNamesByUserId(userId)).isEmpty();
    }

    @DisplayName("WITHDRAWN, DELETED 유저는 업데이트에 실패하고 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404_whenUserStatusIsWithdrawnOrDeleted(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isNotFound());
    }

    @DisplayName("유저가 존재하지 않으면 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isNotFound());
    }

    @DisplayName("닉네임이 이미 존재하면 업데이트에 실패하고 409를 반환한다.")
    @Test
    void shouldReturn409_whenNicknameAlreadyExists() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        UUID otherUserId = UUID.randomUUID();
        userRepository.save(User.builder()
                .id(otherUserId)
                .email("other@test.com")
                .password("password1234!")
                .name("다른 유저")
                .nickname("새닉네임")
                .birth(LocalDate.of(2000, 1, 1))
                .gender(Gender.M)
                .country("KR")
                .weight(70.0)
                .height(175.0)
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(false)
                .status(UserStatus.ACTIVE)
                .build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nickname already exists."));
    }

    @DisplayName("닉네임이 WITHDRAWN, DELETED 유저의 것이면 업데이트에 성공한다.")
    @ParameterizedTest(name = "[{index}] 계정 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldSucceedUpdate_whenNicknameIsUsedByWithdrawnOrDeletedUser(UserStatus nicknameOwnerStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        UUID otherUserId = UUID.randomUUID();
        userRepository.save(User.builder()
                .id(otherUserId)
                .email("other@test.com")
                .password("password1234!")
                .name("다른 유저")
                .nickname("새닉네임")
                .birth(LocalDate.of(2000, 1, 1))
                .gender(Gender.M)
                .country("KR")
                .weight(70.0)
                .height(175.0)
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(false)
                .status(nicknameOwnerStatus)
                .build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isOk());
    }

    @DisplayName("토큰이 없으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        //when
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("이름이 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder().name(null).build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name is required."));
    }

    @DisplayName("이름이 2자 미만이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsTooShort() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder().name("김").build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name must be between 2 and 20 characters."));
    }

    @DisplayName("닉네임이 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenNicknameIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder().nickname(null).build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nickname is required."));
    }

    @DisplayName("생년월일이 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenBirthIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder().birth(null).build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date of birth is required."));
    }

    @DisplayName("성별이 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenGenderIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder().gender(null).build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gender is required."));
    }

    @DisplayName("국가가 null이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenCountryIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateUserInfoRequest request = createValidRequest().toBuilder().country(null).build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Country is required."));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private UpdateUserInfoRequest createValidRequest() {
        return UpdateUserInfoRequest.builder()
                .name("업데이트 이름")
                .nickname("새닉네임")
                .weight(75.0)
                .height(180.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("diabetes"))
                .allergies(List.of("milk"))
                .isMarketingTermsAgreed(true)
                .build();
    }

    private User getUser(UUID userId, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .password("password1234!")
                .name("테스트 유저")
                .nickname("닉네임")
                .birth(LocalDate.of(2002, 5, 5))
                .gender(Gender.F)
                .country("KR")
                .weight(70.0)
                .height(175.0)
                .provider(Provider.LOCAL)
                .isMarketingTermsAgreed(true)
                .status(userStatus)
                .build();
    }

}

