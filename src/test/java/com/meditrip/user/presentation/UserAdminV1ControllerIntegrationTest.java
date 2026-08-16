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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserAdminV1ControllerIntegrationTest {

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

    @DisplayName("ADMIN 권한 토큰으로 요청하면 기간 내 가입한 유저 목록을 페이징으로 반환한다.")
    @Test
    void shouldReturnSignupStatistics_whenRequestedByAdmin() throws Exception {
        //given
        User insideRange = createUser("inside@test.com");
        User outsideRange = createUser("outside@test.com");

        setCreatedAt(insideRange.getId(), LocalDate.of(2026, 8, 10));
        setCreatedAt(outsideRange.getId(), LocalDate.of(2026, 1, 1));

        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/signups")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value("inside@test.com"));
    }

    @DisplayName("USER 권한 토큰으로 요청하면 403을 반환한다.")
    @Test
    void shouldReturnForbidden_whenRequestedByNonAdmin() throws Exception {
        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/signups")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @DisplayName("토큰 없이 요청하면 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenTokenMissing() throws Exception {
        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/signups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("startDate, endDate 없이 요청하면 기본 기간(전체~오늘)으로 조회한다.")
    @Test
    void shouldUseDefaultDateRange_whenDateParamsOmitted() throws Exception {
        //given
        User user = createUser("default@test.com");
        setCreatedAt(user.getId(), LocalDate.now());

        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/signups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @DisplayName("size 파라미터에 맞게 페이징이 적용된다.")
    @Test
    void shouldApplyPaging_whenSizeSpecified() throws Exception {
        //given
        for (int i = 0; i < 5; i++) {
            User user = createUser("user" + i + "@test.com");
            setCreatedAt(user.getId(), LocalDate.now());
        }

        //when, then
        mockMvc.perform(get("/api/admin/v1/users/statistics/signups")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "2")
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    private User createUser(String email) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .provider(Provider.GOOGLE)
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.USER)
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
