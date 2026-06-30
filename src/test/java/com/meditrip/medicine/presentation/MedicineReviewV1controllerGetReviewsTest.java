package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.enums.UserStatus;
import com.meditrip.user.domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MedicineReviewV1controllerGetReviewsTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineReviewRepository medicineReviewRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        medicineRepository.deleteAllInBatch();
        medicineReviewRepository.deleteAllInBatch();
    }

    private Medicine persistMedicine() {
        return medicineRepository.save(Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build());
    }

    private MedicineReview persistReview(Long medicineId, UUID userId, Double rating, String gender,
                                         String country, String symptom) {
        MedicineReview review = MedicineReview.create(
                medicineId, "약이 정말 좋네요.", 25, 170.0, 60.0, rating, gender, country, userId, symptom);
        return medicineReviewRepository.save(review);
    }

    @DisplayName("약과 리뷰가 존재하면 리뷰 목록을 조회할 수 있다.")
    @Test
    void shouldReturnReviewsSuccessfully_whenMedicineAndReviewsExist() throws Exception {
        //given
        UUID requestUserId = UUID.randomUUID();
        User requestUser = createUser(requestUserId, UserStatus.ACTIVE);
        userRepository.save(requestUser);

        UUID authorId = UUID.randomUUID();
        User author = createUser(authorId, "author@test.com", "password123!", UserStatus.ACTIVE);
        userRepository.save(author);

        Medicine medicine = persistMedicine();
        MedicineReview review = persistReview(
                medicine.getId(), authorId, 5.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(requestUserId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode items = root.get("items");

        assertThat(items).hasSize(1);
        JsonNode item = items.get(0);
        assertThat(item.get("id").asLong()).isEqualTo(review.getId());
        assertThat(item.get("nickname").asText()).isEqualTo(author.getNickname());
        assertThat(item.get("authorGender").asText()).isEqualTo("Female");
        assertThat(item.get("authorAgeGroup").asText()).isEqualTo("20s");
        assertThat(item.get("authorRegion").asText()).isEqualTo("KR");
        assertThat(item.get("rating").asDouble()).isEqualTo(5.0);
        assertThat(item.get("symptoms").get(0).asText()).isEqualTo("Headache");
        assertThat(item.get("isAuthor").asBoolean()).isFalse();
        assertThat(item.get("review").asText()).isEqualTo("약이 정말 좋네요.");
        assertThat(root.get("hasNext").asBoolean()).isFalse();
        assertThat(root.get("nextCursor").isNull()).isTrue();
    }

    @DisplayName("본인이 작성한 리뷰를 조회하면 isAuthor가 true로 반환된다.")
    @Test
    void shouldReturnIsAuthorTrue_whenReviewBelongsToRequestUser() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(root.get("items").get(0).get("isAuthor").asBoolean()).isTrue();
    }

    @DisplayName("size를 지정하지 않으면 기본값(15)으로 조회되어, 15개를 초과하는 리뷰가 있으면 hasNext가 true다.")
    @Test
    void shouldUseDefaultSizeFifteen_whenSizeIsNotSpecified() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        for (int i = 0; i < 16; i++) {
            persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        }

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(root.get("items")).hasSize(15);
        assertThat(root.get("hasNext").asBoolean()).isTrue();
        assertThat(root.get("nextCursor").asText()).isNotBlank();
    }

    @DisplayName("size를 지정하면 해당 개수만큼 조회된다.")
    @Test
    void shouldReturnRequestedSizeOfReviews_whenSizeIsSpecified() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        for (int i = 0; i < 5; i++) {
            persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        }

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews?size=3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(root.get("items")).hasSize(3);
        assertThat(root.get("hasNext").asBoolean()).isTrue();
    }

    @DisplayName("cursor를 지정하면 해당 id 이후(LATEST 기준 더 작은 id)의 리뷰부터 조회된다.")
    @Test
    void shouldReturnReviewsAfterCursor_whenCursorIsSpecified() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview first = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        MedicineReview second = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        MedicineReview third = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews"
                        + "?cursor=" + third.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode items = root.get("items");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("id").asLong()).isEqualTo(second.getId());
        assertThat(items.get(1).get("id").asLong()).isEqualTo(first.getId());
    }

    @DisplayName("sort=HIGHEST_RATING이면 평점이 높은 순으로 정렬되어 조회된다.")
    @Test
    void shouldReturnReviewsOrderedByHighestRating_whenSortIsHighestRating() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview low = persistReview(medicine.getId(), userId, 1.0, "Female", "KR", "Headache");
        MedicineReview high = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews" + "?sort=HIGHEST_RATING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items.get(0).get("id").asLong()).isEqualTo(high.getId());
        assertThat(items.get(1).get("id").asLong()).isEqualTo(low.getId());
    }

    @DisplayName("sort=LOWEST_RATING이면 평점이 낮은 순으로 정렬되어 조회된다.")
    @Test
    void shouldReturnReviewsOrderedByLowestRating_whenSortIsLowestRating() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview low = persistReview(medicine.getId(), userId, 1.0, "Female", "KR", "Headache");
        MedicineReview high = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews" + "?sort=LOWEST_RATING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items.get(0).get("id").asLong()).isEqualTo(low.getId());
        assertThat(items.get(1).get("id").asLong()).isEqualTo(high.getId());
    }

    @DisplayName("sort를 지정하지 않으면 기본값 LATEST로 최신순 조회된다.")
    @Test
    void shouldUseDefaultSortLatest_whenSortIsNotSpecified() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview first = persistReview(medicine.getId(), userId, 3.0, "Female", "KR", "Headache");
        MedicineReview second = persistReview(medicine.getId(), userId, 1.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items.get(0).get("id").asLong()).isEqualTo(second.getId());
        assertThat(items.get(1).get("id").asLong()).isEqualTo(first.getId());
    }

    @DisplayName("gender 파라미터로 필터링하면 해당 gender의 리뷰만 조회된다.")
    @Test
    void shouldFilterReviewsByGenderParameter() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview femaleReview = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        persistReview(medicine.getId(), userId, 5.0, "Male", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews"+ "?gender=Female")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("id").asLong()).isEqualTo(femaleReview.getId());
    }

    @DisplayName("countries 파라미터로 필터링하면 해당 국가의 리뷰만 조회된다.")
    @Test
    void shouldFilterReviewsByCountriesParameter() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview krReview = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        persistReview(medicine.getId(), userId, 5.0, "Female", "US", "Headache");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews"+ "?countries=KR")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("id").asLong()).isEqualTo(krReview.getId());
    }

    @DisplayName("symptoms 파라미터로 필터링하면 해당 증상의 리뷰만 조회된다.")
    @Test
    void shouldFilterReviewsBySymptomsParameter() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview headacheReview = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Fever");

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews" + "?symptoms=Headache")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("id").asLong()).isEqualTo(headacheReview.getId());
    }

    @DisplayName("gender, countries, symptoms를 배열로 동시에 전달하면 다중값 OR/AND 조건으로 필터링된다.")
    @Test
    void shouldFilterReviewsByMultipleArrayParameters() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();
        MedicineReview matchedFemale = persistReview(medicine.getId(), userId, 5.0, "Female", "KR", "Headache");
        MedicineReview matchedMale = persistReview(medicine.getId(), userId, 5.0, "Male", "US", "Fever");
        persistReview(medicine.getId(), userId, 5.0, "Female", "JP", "Headache"); // country 불일치

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews"
                        + "?gender=Female&gender=Male&countries=KR&countries=US"
                        + "&symptoms=Headache&symptoms=Fever")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items).hasSize(2);
        assertThat(items)
                .extracting(item -> item.get("id").asLong())
                .containsExactlyInAnyOrder(matchedFemale.getId(), matchedMale.getId());
    }

    @DisplayName("조회된 리뷰가 없으면 빈 items와 hasNext false를 200으로 반환한다.")
    @Test
    void shouldReturnEmptyItems_whenNoReviewsExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = persistMedicine();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(root.get("items")).isEmpty();
        assertThat(root.get("hasNext").asBoolean()).isFalse();
        assertThat(root.get("nextCursor").isNull()).isTrue();
    }

    @DisplayName("조회할 약이 존재하지 않으면 404를 반환하고, 리뷰 조회에 실패한다.")
    @Test
    void shouldReturn404_whenMedicineDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(get("/api/v1/medicines/" + 999L + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Medicine Not Found"));
    }

    @DisplayName("토큰 없이 요청하면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsMissing() throws Exception {
        //given
        Medicine medicine = persistMedicine();

        //when, then
        mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("탈퇴했거나 삭제된 유저, 게스트 유저가 요청하면 리뷰 작성자 닉네임 조회와 무관하게 정상적으로 조회된다 (요청자 본인 상태는 검증하지 않음).")
    @Test
    void shouldReturnReviews_regardlessOfRequestUserStatus() throws Exception {
        //given
        UUID requestUserId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User author = createUser(authorId, "author@test.com", "password123!", UserStatus.ACTIVE);
        userRepository.save(author);

        Medicine medicine = persistMedicine();
        persistReview(medicine.getId(), authorId, 5.0, "Female", "KR", "Headache");

        String accessToken = jwtProvider.generateAccessToken(requestUserId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/" + medicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("isAuthor").asBoolean()).isFalse();
    }

}
