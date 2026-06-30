package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.AllergyRepository;
import com.meditrip.user.domain.repository.ConditionRepository;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import com.meditrip.user.presentation.dto.request.OnboardingRequest;
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

class UserV1ControllerOnboardingTest extends ControllerTestSupport {

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

    @DisplayName("GUEST 유저가 온보딩에 성공하면 200을 반환한다.")
    @Test
    void shouldReturn200_whenGuestUserOnboardsSuccessfully() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest();

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/users/onboarding")
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

    @DisplayName("온보딩 성공 시 유저 status가 ACTIVE로 변경된다.")
    @Test
    void shouldChangeStatusToActive_whenOnboardingSucceeds() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isOk());

        //then
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("온보딩 시 conditions와 allergies가 DB에 저장된다.")
    @Test
    void shouldSaveConditionsAndAllergies_whenOnboardingSucceeds() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder()
                .underlyingDisease(List.of("diabetes", "asthma"))
                .allergies(List.of("milk", "eggs"))
                .build();

        //when
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        //then
        List<String> conditions = userConditionRepository.findConditionNamesByUserId(userId);
        List<String> allergies = userAllergyRepository.findAllergyNamesByUserId(userId);

        assertThat(conditions).containsExactlyInAnyOrder("diabetes", "asthma");
        assertThat(allergies).containsExactlyInAnyOrder("milk", "eggs");
    }

    @DisplayName("conditions, allergies가 null이면 빈 리스트로 반환된다.")
    @Test
    void shouldReturnEmptyLists_whenConditionsAndAllergiesAreNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder()
                .underlyingDisease(null)
                .allergies(null)
                .build();

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/users/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

        assertThat(resultNode.get("underlyingDisease").isEmpty()).isTrue();
        assertThat(resultNode.get("allergies").isEmpty()).isTrue();
    }

    @DisplayName("GUEST가 아닌 유저가 온보딩하면 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태: {0}")
    @EnumSource(value = UserStatus.class, names = {"ACTIVE", "WITHDRAWN", "DELETED"})
    void shouldReturn403_whenUserIsNotGuest(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getUser(userId, userStatus));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to access this page."));
    }

    @DisplayName("유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isNotFound());
    }

    @DisplayName("토큰이 없으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        userRepository.save(getGuestUser(userId));

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        //when
        mockMvc.perform(post("/api/v1/users/onboarding")
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
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder().name(null).build();

        mockMvc.perform(post("/api/v1/users/onboarding")
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
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder().name("김").build();

        mockMvc.perform(post("/api/v1/users/onboarding")
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
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder().nickname(null).build();

        mockMvc.perform(post("/api/v1/users/onboarding")
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
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder().birth(null).build();

        mockMvc.perform(post("/api/v1/users/onboarding")
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
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder().gender(null).build();

        mockMvc.perform(post("/api/v1/users/onboarding")
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
        userRepository.save(getGuestUser(userId));
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        OnboardingRequest request = createValidRequest().toBuilder().country(null).build();

        mockMvc.perform(post("/api/v1/users/onboarding")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Country is required."));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private OnboardingRequest createValidRequest() {
        return OnboardingRequest.builder()
                .name("테스트 유저")
                .nickname("닉네임")
                .weight(70.0)
                .height(175.0)
                .birth(LocalDate.of(2002, 5, 5))
                .gender("F")
                .country("KR")
                .underlyingDisease(List.of("diabetes"))
                .allergies(List.of("milk"))
                .isMarketingTermsAgreed(true)
                .build();
    }

    private User getGuestUser(UUID userId) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
                .name("소셜 유저")
                .provider(Provider.GOOGLE)
                .isMarketingTermsAgreed(false)
                .status(UserStatus.GUEST)
                .build();
    }

    private User getUser(UUID userId, UserStatus userStatus) {
        return User.builder()
                .id(userId)
                .email("test@test.com")
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
