package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Ingredient;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIngredients;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.HashtagRepository;
import com.meditrip.medicine.domain.repository.IngredientRepository;
import com.meditrip.medicine.domain.repository.MedicineHashtagsRepository;
import com.meditrip.medicine.domain.repository.MedicineIngredientsRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TakeMedicineV1ControllerGetMedicineIntakesTest extends ControllerTestSupport {

    @Autowired
    private MedicineIntakeRepository medicineIntakeRepository;

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
        medicineIntakeRepository.deleteAllInBatch();
        medicineHashtagsRepository.deleteAllInBatch();
        medicineIngredientsRepository.deleteAllInBatch();
        hashtagRepository.deleteAllInBatch();
        ingredientRepository.deleteAllInBatch();
        medicineRepository.deleteAllInBatch();
    }

    private Medicine persistMedicine(String nameEn) {
        return medicineRepository.save(Medicine.builder()
                .nameKo("타이레놀")
                .nameEn(nameEn)
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

    private void persistIngredientAndHashtags(Long medicineId) {
        Ingredient ingredient = ingredientRepository.save(Ingredient.builder()
                .nameEn("Acetaminophen")
                .nameKo("아세트아미노펜")
                .build());

        medicineIngredientsRepository.save(MedicineIngredients.builder()
                .medicineId(medicineId)
                .ingredientId(ingredient.getId())
                .amount("500mg")
                .build());

        Hashtag diseaseHashtag = hashtagRepository.save(Hashtag.builder()
                .name("headache")
                .type(HashtagType.DISEASE)
                .build());

        medicineHashtagsRepository.save(MedicineHashtags.builder()
                .medicineId(medicineId)
                .hashtagId(diseaseHashtag.getId())
                .build());
    }

    private MedicineIntake persistIntake(Long medicineId, UUID userId, Instant firstTakenAt) {
        return medicineIntakeRepository.save(MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(firstTakenAt)
                .build());
    }

    @DisplayName("복약 이력을 조회하면 각 이력에 약 정보가 함께 채워져 200으로 반환된다.")
    @Test
    void shouldReturnMedicineIntakesWithMedicineInfo_whenIntakesExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        Medicine medicine = persistMedicine("Tylenol");
        persistIngredientAndHashtags(medicine.getId());
        MedicineIntake intake = persistIntake(medicine.getId(), userId, Instant.now());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("content")).hasSize(1);
        JsonNode item = resultNode.get("content").get(0);
        assertThat(item.get("intakeId").asLong()).isEqualTo(intake.getId());
        assertThat(item.get("firstTakenAt").asText()).isNotBlank();
        assertThat(item.get("medicine").get("id").asLong()).isEqualTo(medicine.getId());
        assertThat(item.get("medicine").get("name").asText()).isEqualTo("Tylenol");
        assertThat(item.get("medicine").get("ingredients").get(0).get("ingredientName").asText())
                .isEqualTo("Acetaminophen");
        assertThat(item.get("medicine").get("diseaseHashtags").get(0).asText()).isEqualTo("headache");

        assertThat(resultNode.get("page").asInt()).isEqualTo(1);
        assertThat(resultNode.get("size").asInt()).isEqualTo(10);
        assertThat(resultNode.get("totalElements").asInt()).isEqualTo(1);
        assertThat(resultNode.get("totalPages").asInt()).isEqualTo(1);
    }

    @DisplayName("복약 이력이 하나도 없으면 빈 목록과 함께 200을 반환한다.")
    @Test
    void shouldReturnEmptyItems_whenNoIntakesExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("content")).isEmpty();
        assertThat(resultNode.get("totalElements").asInt()).isZero();
    }

    @DisplayName("다른 유저의 복약 이력은 조회되지 않는다.")
    @Test
    void shouldNotReturnOtherUsersIntakes() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        Medicine medicine = persistMedicine("Tylenol");

        persistIntake(medicine.getId(), anotherUserId, Instant.now());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("content")).isEmpty();
    }

    @DisplayName("firstStartDate, firstEndDate로 필터링하면 해당 기간에 첫 복용한 이력만 반환된다.")
    @Test
    void shouldFilterIntakesByDateRange() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        Medicine medicine = persistMedicine("Tylenol");

        Instant withinRange = Instant.parse("2024-05-15T00:00:00Z");
        Instant outOfRange = Instant.parse("2023-01-01T00:00:00Z");

        MedicineIntake withinIntake = persistIntake(medicine.getId(), userId, withinRange);
        persistIntake(medicine.getId(), userId, outOfRange);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("firstStartDate", "2024-01-01")
                        .param("firstEndDate", "2024-12-31"))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("content")).hasSize(1);
        assertThat(resultNode.get("content").get(0).get("intakeId").asLong()).isEqualTo(withinIntake.getId());
    }

    @DisplayName("size만큼 페이지가 나뉘어 조회되고, 요청한 page의 결과만 반환된다.")
    @Test
    void shouldReturnPagedResult_whenIntakeCountExceedsSize() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        Medicine medicine = persistMedicine("Tylenol");

        for (int i = 0; i < 3; i++) {
            persistIntake(medicine.getId(), userId, Instant.now().minusSeconds(i * 1000L));
        }

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "2"))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("content")).hasSize(2);
        assertThat(resultNode.get("totalElements").asInt()).isEqualTo(3);
        assertThat(resultNode.get("totalPages").asInt()).isEqualTo(2);
        assertThat(resultNode.get("page").asInt()).isEqualTo(1);
    }

    @DisplayName("page=1(0-indexed)로 요청하면 두 번째 페이지 결과가 조회되고, 응답 page 필드는 2(1-indexed)로 내려온다.")
    @Test
    void shouldReturnSecondPage_whenPageParamIsOne() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        Medicine medicine = persistMedicine("Tylenol");

        for (int i = 0; i < 3; i++) {
            persistIntake(medicine.getId(), userId, Instant.now().minusSeconds(i * 1000L));
        }

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("page", "1")
                        .param("size", "2"))
                //then
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(resultNode.get("content")).hasSize(1);
        assertThat(resultNode.get("page").asInt()).isEqualTo(2);
    }

    @DisplayName("토큰이 없으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/medicines/me"))
                .andExpect(status().isUnauthorized());
    }

}
