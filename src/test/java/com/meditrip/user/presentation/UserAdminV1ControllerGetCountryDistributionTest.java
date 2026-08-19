package com.meditrip.user.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.domain.UserRole;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserAdminV1ControllerGetCountryDistributionTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private EntityManager entityManager;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtProvider.generateAccessToken(UUID.randomUUID().toString(), UserRole.ADMIN);
        userToken = jwtProvider.generateAccessToken(UUID.randomUUID().toString(), UserRole.USER);
    }

    @DisplayName("ADMIN 권한 토큰으로 요청하면 국가별 유저 목록을 페이징으로 반환한다.")
    @Test
    void shouldReturnUsersByCountryWithPaging_whenRequestedByAdmin() throws Exception {
        //given
        String targetCountry = "KR";
        String otherCountry = "US";

        createUser("kr-user@test.com", targetCountry);
        createUser("us-user@test.com", otherCountry);

        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/distribution/country")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("country", targetCountry)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("kr-user@test.com"));
    }

    @DisplayName("ADMIN 권한 토큰으로 요청하면 국가 및 회원가입 날짜도 선택해서 조회할 수 있다.")
    @Test
    void shouldReturnUsersByCountryAndDateRange_whenRequestedByAdmin() throws Exception {
        //given
        String targetCountry = "KR";
        String otherCountry = "US";

        User targetCountryUser = createUser("target-user@test.com", targetCountry);
        User otherCountryUser = createUser("us-user@test.com", otherCountry);

        setCreatedAt(targetCountryUser.getId(), LocalDate.of(2026, 8, 10));
        setCreatedAt(otherCountryUser.getId(), LocalDate.of(2026, 1, 1));

        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/distribution/country")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("country", targetCountry)
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("target-user@test.com"));
    }

    @DisplayName("USER 권한 토큰으로 요청하면 403을 반환한다.")
    @Test
    void shouldReturnForbidden_whenRequestedByNonAdmin() throws Exception {
        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/distribution/country")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @DisplayName("토큰 없이 요청하면 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenTokenMissing() throws Exception {
        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/distribution/country")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("size 파라미터에 맞게 페이징이 적용된다.")
    @Test
    void shouldApplyPaging_whenSizeSpecified() throws Exception {
        //given
        String targetCountry = "KR";

        for (int i = 0; i < 5; i++) {
            User user = createUser("user" + i + "@test.com", targetCountry);
            setCreatedAt(user.getId(), LocalDate.now());
        }

        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/distribution/country")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("country", targetCountry)
                        .param("size", "2")
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    private User createUser(String email, String country) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .provider(Provider.GOOGLE)
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.USER)
                .country(country)
                .build();
        return userRepository.save(user);
    }

    private void setCreatedAt(UUID userId, LocalDate date) {
        Instant instant = date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        entityManager.createNativeQuery("UPDATE users SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", instant)
                .setParameter("id", userId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

}
