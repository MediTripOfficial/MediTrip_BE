package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Ingredient;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIngredients;
import com.meditrip.medicine.domain.repository.HashtagRepository;
import com.meditrip.medicine.domain.repository.IngredientRepository;
import com.meditrip.medicine.domain.repository.MedicineHashtagsRepository;
import com.meditrip.medicine.domain.repository.MedicineIngredientsRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.user.presentation.ControllerTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MedicineV1ControllerGetMedicineInfoTest extends ControllerTestSupport {

    @Autowired
    private JwtProvider jwtProvider;

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

    @AfterEach
    void tearDown() {
        medicineRepository.deleteAllInBatch();
        ingredientRepository.deleteAllInBatch();
        hashtagRepository.deleteAllInBatch();
        medicineIngredientsRepository.deleteAllInBatch();
        medicineHashtagsRepository.deleteAllInBatch();
    }

    @DisplayName("약 ID로 조회하면 약 상세 정보를 반환한다.")
    @Test
    void shouldReturnMedicineInfo_whenMedicineExists() throws Exception {
        //given
        Medicine medicine = medicineRepository.save(Medicine.builder()
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

        String jsonResponse = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resultNode = objectMapper.readTree(jsonResponse);

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
        assertThat(resultNode.get("topReview").isNull()).isTrue();
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
