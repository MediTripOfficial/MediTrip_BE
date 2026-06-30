package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;

class UserV1ControllerGetUserInfoTest extends ControllerTestSupport {

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

    @DisplayName("유저 정보가 존재하고 ACTIVE, GUEST 상태라면 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "GUEST"})
    void shouldSucceedRetrieval_whenUserExistsAndStatusIsActiveOrGuest(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        User user = userRepository.findById(userId).orElseThrow();
        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

        assertThat(resultNode.get("email").asText()).isEqualTo(user.getEmail());
        assertThat(resultNode.get("name").asText()).isEqualTo(user.getName());
        assertThat(resultNode.get("nickname").asText()).isEqualTo(user.getNickname());
        assertThat(resultNode.get("birth").asText()).isEqualTo(user.getBirth().toString());
        assertThat(resultNode.get("gender").asText()).isEqualTo(user.getGender().getEng());
        assertThat(resultNode.get("country").asText()).isEqualTo(user.getCountry());
        assertThat(resultNode.get("weight").asDouble()).isEqualTo(user.getWeight());
        assertThat(resultNode.get("height").asDouble()).isEqualTo(user.getHeight());
    }

    @DisplayName("기저질환과 알레르기가 있으면 함께 반환된다.")
    @Test
    void shouldReturnConditionsAndAllergies_whenUserHasConditionsAndAllergies() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        Condition condition = conditionRepository.save(Condition.create("DIABETES"));
        Allergy allergy = allergyRepository.save(Allergy.create("Milk"));

        userConditionRepository.save(UserCondition.builder()
                .userId(userId)
                .conditionId(condition.getId())
                .build());
        userAllergyRepository.save(UserAllergy.builder()
                .userId(userId)
                .allergyId(allergy.getId())
                .build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

        assertThat(resultNode.get("underlyingDisease").get(0).asText()).isEqualTo("DIABETES");
        assertThat(resultNode.get("allergies").get(0).asText()).isEqualTo("Milk");
    }

    @DisplayName("기저질환과 알레르기가 없으면 빈 리스트로 반환된다.")
    @Test
    void shouldReturnEmptyLists_whenUserHasNoConditionsAndAllergies() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

        assertThat(resultNode.get("underlyingDisease").isEmpty()).isTrue();
        assertThat(resultNode.get("allergies").isEmpty()).isTrue();
    }

    @DisplayName("기저질환이 여러 개면 전부 반환된다.")
    @Test
    void shouldReturnAllConditions_whenUserHasMultipleConditions() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, UserStatus.ACTIVE));

        Condition condition1 = conditionRepository.save(Condition.create("DIABETES"));
        Condition condition2 = conditionRepository.save(Condition.create("ASTHMA"));

        userConditionRepository.saveAll(List.of(
                UserCondition.builder().userId(userId).conditionId(condition1.getId()).build(),
                UserCondition.builder().userId(userId).conditionId(condition2.getId()).build()
        ));

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

        assertThat(resultNode.get("underlyingDisease")).hasSize(2);
    }

    @DisplayName("WITHDRAWN, DELETED 유저는 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"WITHDRAWN", "DELETED"})
    void shouldReturn404_whenUserStatusIsWithdrawnOrDeleted(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"));
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found"));
    }

    // ==================== 인증 ====================

    @DisplayName("토큰이 없으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
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
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 공통 ====================

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
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
