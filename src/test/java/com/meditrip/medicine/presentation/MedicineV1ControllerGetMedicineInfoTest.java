package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Ingredient;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIngredients;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.HashtagRepository;
import com.meditrip.medicine.domain.repository.IngredientRepository;
import com.meditrip.medicine.domain.repository.MedicineHashtagsRepository;
import com.meditrip.medicine.domain.repository.MedicineIngredientsRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MedicineV1ControllerGetMedicineInfoTest extends ControllerTestSupport {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private MedicineIngredientsRepository medicineIngredientsRepository;

    @Autowired
    private MedicineHashtagsRepository medicineHashtagsRepository;

    @Autowired
    private MedicineReviewRepository medicineReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        medicineHashtagsRepository.deleteAllInBatch();
        medicineIngredientsRepository.deleteAllInBatch();
        medicineReviewRepository.deleteAllInBatch();
        hashtagRepository.deleteAllInBatch();
        ingredientRepository.deleteAllInBatch();
        medicineRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private Medicine persistMedicine() {
        return medicineRepository.save(Medicine.builder()
                .nameKo("타이레놀")
                .nameEn("Tylenol")
                .manufacturerKo("존슨앤드존슨")
                .manufacturerEn("Johnson & Johnson")
                .isConvenienceStore(true)
                .isChildSafe(true)
                .dosage("1 tablet")
                .dosageInterval("4-6 hours")
                .maxLimit("8 tablets/day")
                .caution("Avoid alcohol")
                .usageDetailEn("Take with water")
                .drugInteractionsEn("None known")
                .seeDoctorEn("If symptoms persist over 3 days")
                .imageUrl("https://example.com/tylenol.png")
                .countryCode("US")
                .build());
    }

    private MedicineReview persistReview(Long medicineId, UUID authorId, Double rating) {
        MedicineReview review = MedicineReview.create(
                medicineId, "약이 정말 좋네요.", 28, 165.0, 55.0, rating, "Female", "KR", authorId, "Headache");
        return medicineReviewRepository.save(review);
    }

    @DisplayName("약 ID로 조회하면 약 상세 정보를 반환한다.")
    @Test
    void shouldReturnMedicineInfo_whenMedicineExists() throws Exception {
        //given
        Medicine medicine = persistMedicine();

        Ingredient ingredient = ingredientRepository.save(Ingredient.builder()
                .nameEn("Acetaminophen")
                .nameKo("아세트아미노펜")
                .build());

        medicineIngredientsRepository.save(MedicineIngredients.builder()
                .medicineId(medicine.getId())
                .ingredientId(ingredient.getId())
                .amount("500mg")
                .build());

        Hashtag diseaseHashtag = hashtagRepository.save(Hashtag.builder()
                .name("headache")
                .type(HashtagType.DISEASE)
                .build());

        Hashtag efficacyHashtag = hashtagRepository.save(Hashtag.builder()
                .name("painRelief")
                .type(HashtagType.EFFICACY)
                .build());

        medicineHashtagsRepository.save(MedicineHashtags.builder()
                .medicineId(medicine.getId())
                .hashtagId(diseaseHashtag.getId())
                .build());

        medicineHashtagsRepository.save(MedicineHashtags.builder()
                .medicineId(medicine.getId())
                .hashtagId(efficacyHashtag.getId())
                .build());

        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/{medicineId}", medicine.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("id").asLong()).isEqualTo(medicine.getId());
        assertThat(resultNode.get("name").asText()).isEqualTo("Tylenol");
        assertThat(resultNode.get("manufacturer").asText()).isEqualTo("Johnson & Johnson");
        assertThat(resultNode.get("ingredients").get(0).get("ingredientName").asText()).isEqualTo("Acetaminophen");
        assertThat(resultNode.get("ingredients").get(0).get("amount").asText()).isEqualTo("500mg");
        assertThat(resultNode.get("diseaseHashtags").get(0).asText()).isEqualTo("headache");
        assertThat(resultNode.get("efficacyHashtags").get(0).asText()).isEqualTo("painRelief");
        assertThat(resultNode.get("isConvenienceStore").asBoolean()).isTrue();
        assertThat(resultNode.get("purchaseLocation").get(0).asText()).isEqualTo("store");
        assertThat(resultNode.get("purchaseLocation").get(1).asText()).isEqualTo("pharmacy");
    }

    @DisplayName("리뷰가 하나도 없는 약을 조회해도 500 없이 200을 반환하고, rating=null, reviewCount=0, topReview=null이다.")
    @Test
    void shouldReturn200WithNullRating_whenMedicineHasNoReviews() throws Exception {
        //given
        Medicine medicine = persistMedicine();

        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/{medicineId}", medicine.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("rating").isNull()).isTrue();
        assertThat(resultNode.get("reviewCount").asInt()).isZero();
        assertThat(resultNode.get("topReview").isNull()).isTrue();
    }

    @DisplayName("리뷰가 존재하면 평균 평점/개수가 채워지고, 가장 최근 리뷰가 topReview로 작성자 닉네임/프로필과 함께 내려온다.")
    @Test
    void shouldReturnRatingAndTopReview_whenReviewsExist() throws Exception {
        //given
        Medicine medicine = persistMedicine();

        UUID authorId = UUID.randomUUID();
        User author = createUser(authorId, "author@test.com", "password123!", UserStatus.ACTIVE);
        userRepository.save(author);

        persistReview(medicine.getId(), authorId, 4.0);
        MedicineReview latestReview = persistReview(medicine.getId(), authorId, 2.0);

        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/{medicineId}", medicine.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("rating").asDouble()).isEqualTo(3.0);
        assertThat(resultNode.get("reviewCount").asInt()).isEqualTo(2);

        JsonNode topReview = resultNode.get("topReview");
        assertThat(topReview.isNull()).isFalse();
        assertThat(topReview.get("id").asLong()).isEqualTo(latestReview.getId());
        assertThat(topReview.get("nickname").asText()).isEqualTo(author.getNickname());
        assertThat(topReview.get("authorGender").asText()).isEqualTo("Female");
        assertThat(topReview.get("authorRegion").asText()).isEqualTo("KR");
        assertThat(topReview.get("rating").asDouble()).isEqualTo(2.0);
    }

    @DisplayName("삭제된 리뷰는 평점/개수 계산과 topReview 선정에서 제외된다.")
    @Test
    void shouldExcludeDeletedReviews_fromRatingAndTopReview() throws Exception {
        //given
        Medicine medicine = persistMedicine();

        UUID authorId = UUID.randomUUID();
        User author = createUser(authorId, "author@test.com", "password123!", UserStatus.ACTIVE);
        userRepository.save(author);

        MedicineReview activeReview = persistReview(medicine.getId(), authorId, 5.0);

        MedicineReview deletedReview = persistReview(medicine.getId(), authorId, 1.0);
        deletedReview.delete();
        medicineReviewRepository.save(deletedReview);

        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/{medicineId}", medicine.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("rating").asDouble()).isEqualTo(5.0);
        assertThat(resultNode.get("reviewCount").asInt()).isEqualTo(1);
        assertThat(resultNode.get("topReview").get("id").asLong()).isEqualTo(activeReview.getId());
    }

    @DisplayName("리뷰 작성자 계정이 존재하지 않아도(탈퇴 등) 500 없이 200을 반환하고, topReview의 nickname/profileImg는 null이다.")
    @Test
    void shouldReturn200_whenTopReviewAuthorAccountDoesNotExist() throws Exception {
        //given
        Medicine medicine = persistMedicine();

        // 작성자에 대응하는 User row를 의도적으로 저장하지 않음 (탈퇴/하드 삭제 등을 흉내)
        UUID withdrawnAuthorId = UUID.randomUUID();
        persistReview(medicine.getId(), withdrawnAuthorId, 5.0);

        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/{medicineId}", medicine.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("rating").asDouble()).isEqualTo(5.0);
        JsonNode topReview = resultNode.get("topReview");
        assertThat(topReview.isNull()).isFalse();
        assertThat(topReview.get("nickname").asText()).isEqualTo("탈퇴한 유저");
        assertThat(topReview.get("profileImg").isNull()).isTrue();
    }

    @DisplayName("존재하지 않는 약 ID로 조회하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenMedicineDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when, then
        mockMvc.perform(get("/api/v1/medicines/{medicineId}", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Medicine Not Found"));
    }

    @DisplayName("토큰이 없으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/medicines/{medicineId}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("토큰이 만료되었으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String expiredAccessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        //when, then
        mockMvc.perform(get("/api/v1/medicines/{medicineId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken))
                .andExpect(status().isUnauthorized());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

}
