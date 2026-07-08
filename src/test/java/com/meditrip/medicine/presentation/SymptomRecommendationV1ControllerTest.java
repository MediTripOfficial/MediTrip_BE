package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.common.domain.UserStatus;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Ingredient;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIngredients;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.entity.MedicineSymptomCode;
import com.meditrip.medicine.domain.repository.HashtagRepository;
import com.meditrip.medicine.domain.repository.IngredientRepository;
import com.meditrip.medicine.domain.repository.MedicineHashtagsRepository;
import com.meditrip.medicine.domain.repository.MedicineIngredientsRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.medicine.domain.repository.MedicineSymptomCodeRepository;
import com.meditrip.user.domain.entity.Allergy;
import com.meditrip.user.domain.entity.Condition;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.entity.UserAllergy;
import com.meditrip.user.domain.entity.UserCondition;
import com.meditrip.user.domain.entity.enums.Provider;
import com.meditrip.user.domain.repository.AllergyRepository;
import com.meditrip.user.domain.repository.ConditionRepository;
import com.meditrip.user.domain.repository.UserAllergyRepository;
import com.meditrip.user.domain.repository.UserConditionRepository;
import com.meditrip.user.domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SymptomRecommendationV1ControllerTest extends ControllerTestSupport {

    @Autowired
    private JwtProvider jwtProvider;

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
    private MedicineRepository medicineRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private MedicineIngredientsRepository medicineIngredientsRepository;

    @Autowired
    private MedicineSymptomCodeRepository medicineSymptomCodeRepository;

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private MedicineHashtagsRepository medicineHashtagsRepository;

    @Autowired
    private MedicineReviewRepository medicineReviewRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        conditionRepository.deleteAllInBatch();
        allergyRepository.deleteAllInBatch();
        userConditionRepository.deleteAllInBatch();
        userAllergyRepository.deleteAllInBatch();
        medicineRepository.deleteAllInBatch();
        ingredientRepository.deleteAllInBatch();
        medicineIngredientsRepository.deleteAllInBatch();
        medicineSymptomCodeRepository.deleteAllInBatch();
        hashtagRepository.deleteAllInBatch();
        medicineHashtagsRepository.deleteAllInBatch();
        medicineReviewRepository.deleteAllInBatch();
    }

    private User persistUser(String country) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user-" + UUID.randomUUID() + "@test.com")
                .name("테스트유저")
                .nickname("tester-" + UUID.randomUUID().toString().substring(0, 8))
                .country(country)
                .provider(Provider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isMarketingTermsAgreed(false)
                .build();
        return userRepository.save(user);
    }

    private Medicine persistMedicine(String nameEn, String manufacturer, String countryCode,
                                     boolean convenienceStore, int severityTier) {
        Medicine medicine = Medicine.builder()
                .nameEn(nameEn)
                .manufacturerEn(manufacturer)
                .countryCode(countryCode)
                .isConvenienceStore(convenienceStore)
                .severityTier(severityTier)
                .build();
        return medicineRepository.save(medicine);
    }

    private Ingredient persistIngredient(String nameEn) {
        Ingredient ingredient = Ingredient.builder().nameEn(nameEn).nameKo(nameEn).build();
        return ingredientRepository.save(ingredient);
    }

    private void mapMedicineToSymptomCode(Long medicineId, int symptomCode) {
        medicineSymptomCodeRepository.save(MedicineSymptomCode.builder()
                .medicineId(medicineId)
                .symptomCode(symptomCode)
                .build());
    }

    private void mapMedicineToIngredient(Long medicineId, Long ingredientId, String amount) {
        medicineIngredientsRepository.save(MedicineIngredients.builder()
                .medicineId(medicineId)
                .ingredientId(ingredientId)
                .amount(amount)
                .build());
    }

    private Hashtag persistHashtag(String name, HashtagType type) {
        Hashtag hashtag = Hashtag.builder().name(name).type(type).build();
        return hashtagRepository.save(hashtag);
    }

    private void persistReview(Long medicineId, double rating) {
        medicineReviewRepository.save(MedicineReview.builder()
                .medicineId(medicineId)
                .rating(rating)
                .review("약이 정말 좋아요")
                .isDeleted(false)
                .userId(UUID.randomUUID())
                .build());
    }

    private void mapMedicineToHashtag(Long medicineId, Long hashtagId) {
        medicineHashtagsRepository.save(MedicineHashtags.builder()
                .medicineId(medicineId)
                .hashtagId(hashtagId)
                .build());
    }

    private String requestBody(Integer primaryCode, Integer totalScore, String chatting) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "primaryCode", primaryCode,
                "totalScore", totalScore,
                "chatting", chatting
        ));
    }

    @Test
    @DisplayName("사용자가 입력한 증상에 맞는 증상과 약을 추천해준다.")
    void recommend_returns200_withFullSymptomResponse() throws Exception {
        //given
        User user = persistUser("KR");

        Medicine tylenol = persistMedicine("Tylenol", "J&J", "KR", true, 1);
        Ingredient acetaminophen = persistIngredient("Acetaminophen");
        mapMedicineToIngredient(tylenol.getId(), acetaminophen.getId(), "500mg");
        mapMedicineToSymptomCode(tylenol.getId(), 11);

        persistReview(tylenol.getId(), 4.0);
        persistReview(tylenol.getId(), 5.0);

        Hashtag headacheTag = persistHashtag("Headache", HashtagType.DISEASE);
        mapMedicineToHashtag(tylenol.getId(), headacheTag.getId());

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(11, 65, "두통이 있어요")))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode primary = root.get("result").get("primarySymptom");

        assertThat(primary.get("name").asText()).isEqualTo("Fever, Pain & Inflammation");
        assertThat(primary.get("description").asText()).isEqualTo("General & Internal Pain");
        assertThat(primary.get("hashtag").get(0).asText()).isEqualTo("Headache");
        assertThat(primary.get("hashtag").get(1).asText()).isEqualTo("Period Pain");
        assertThat(primary.get("hashtag").get(2).asText()).isEqualTo("Fever");

        JsonNode medicineSummary = primary.get("medicines").get(0);
        assertThat(medicineSummary.get("productNameEng").asText()).isEqualTo("Tylenol");
        assertThat(medicineSummary.get("purchaseLocation").get(0).asText()).isEqualTo("store");
        assertThat(medicineSummary.get("rating").asDouble()).isEqualTo(4.5);
        assertThat(medicineSummary.get("reviewCount").asInt()).isEqualTo(2);

        assertThat(primary.get("similarDrugs")).hasSize(1);

        JsonNode secondary = root.get("result").get("secondarySymptom");
        assertThat(secondary.get("description").asText()).isEqualTo("Muscle & Joint Pain");
        assertThat(secondary.get("medicines")).isEmpty();
    }

    @Test
    @DisplayName("같은 대분류에 형제 소분류가 없는 코드(Eye Irritation)면 secondarySymptom이 null로 내려온다.")
    void recommend_secondarySymptomIsNull_forCodeWithoutSibling() throws Exception {
        //given
        User user = persistUser("KR");
        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(51, 10, "눈이 가려워요")))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(root.get("result").get("secondarySymptom").isNull()).isTrue();
    }

    @Test
    @DisplayName("기저질환(Diabetes)이 있는 유저는 금기 성분이 든 약이 추천 목록에서 빠진다.")
    void recommend_excludesContraindicatedMedicine_forUserWithCondition() throws Exception {
        //given
        User user = persistUser("KR");

        Condition diabetes = conditionRepository.save(Condition.create("Diabetes"));
        userConditionRepository.save(UserCondition.builder()
                .userId(user.getId())
                .conditionId(diabetes.getId())
                .build());

        Medicine sugarSyrup = persistMedicine("SugarSyrupCough", "Co", "KR", true, 1);
        Ingredient sugarIngredient = persistIngredient("Sugar-containing syrup");
        mapMedicineToIngredient(sugarSyrup.getId(), sugarIngredient.getId(), "10ml");
        mapMedicineToSymptomCode(sugarSyrup.getId(), 31);

        Medicine safeMedicine = persistMedicine("SafeLozenge", "Co", "KR", true, 1);
        Ingredient safeIngredient = persistIngredient("Benzocaine");
        mapMedicineToIngredient(safeMedicine.getId(), safeIngredient.getId(), "5mg");
        mapMedicineToSymptomCode(safeMedicine.getId(), 31);

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(31, 50, "기침이 나요")))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode medicines = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("result").get("primarySymptom").get("medicines");

        assertThat(medicines).hasSize(1);
        assertThat(medicines.get(0).get("productNameEng").asText()).isEqualTo("SafeLozenge");
    }

    @Test
    @DisplayName("알레르기(Penicillin)가 있는 유저는 해당 성분이 든 약이 추천 목록에서 빠진다.")
    void recommend_excludesAllergicMedicine_forUserWithAllergy() throws Exception {
        //given
        User user = persistUser("KR");

        Allergy penicillinAllergy = allergyRepository.save(Allergy.create("Penicillin"));
        userAllergyRepository.save(UserAllergy.builder()
                .userId(user.getId())
                .allergyId(penicillinAllergy.getId())
                .build());

        Medicine penicillinOintment = persistMedicine("PenicillinOintment", "Co", "KR", true, 1);
        Ingredient penicillin = persistIngredient("Penicillin");
        mapMedicineToIngredient(penicillinOintment.getId(), penicillin.getId(), "1%");
        mapMedicineToSymptomCode(penicillinOintment.getId(), 41);

        Medicine povidoneOintment = persistMedicine("PovidoneOintment", "Co", "KR", true, 1);
        Ingredient povidone = persistIngredient("Povidone iodine");
        mapMedicineToIngredient(povidoneOintment.getId(), povidone.getId(), "10%");
        mapMedicineToSymptomCode(povidoneOintment.getId(), 41);

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(41, 30, "상처가 났어요")))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode medicines = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("result").get("primarySymptom").get("medicines");

        assertThat(medicines).hasSize(1);
        assertThat(medicines.get(0).get("productNameEng").asText()).isEqualTo("PovidoneOintment");
    }

    @Test
    @DisplayName("사용자 국가와 일치하는 약만 similarDrugs에 노출된다.")
    void recommend_filtersSimilarDrugsByUserCountry() throws Exception {
        //given
        User user = persistUser("KR");

        Medicine krMedicine = persistMedicine("KRBrand", "Co", "KR", true, 1);
        Ingredient krIngredient = persistIngredient("Loratadine");
        mapMedicineToIngredient(krMedicine.getId(), krIngredient.getId(), "10mg");
        mapMedicineToSymptomCode(krMedicine.getId(), 32);

        Medicine usMedicine = persistMedicine("USBrand", "Co", "US", true, 1);
        Ingredient usIngredient = persistIngredient("Cetirizine");
        mapMedicineToIngredient(usMedicine.getId(), usIngredient.getId(), "10mg");
        mapMedicineToSymptomCode(usMedicine.getId(), 32);

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(32, 20, "콧물이 나요")))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode primary = objectMapper
                .readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("result").get("primarySymptom");

        assertThat(primary.get("medicines")).hasSize(2);
        assertThat(primary.get("similarDrugs")).hasSize(1);
        assertThat(primary.get("similarDrugs").get(0).get("productNameEng").asText()).isEqualTo("KRBrand");
    }

    @Test
    @DisplayName("정의되지 않은 primaryCode면 400을 반환한다.")
    void recommend_returns400_whenPrimaryCodeIsInvalid() throws Exception {
        //given
        User user = persistUser("KR");
        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when, then
        mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(999, 10, "test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("999")));
    }

    @Test
    @DisplayName("totalScore가 누락되면 400을 반환한다.")
    void recommend_returns400_whenTotalScoreIsMissing() throws Exception {
        //given
        User user = persistUser("KR");
        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        String invalidBody = objectMapper.writeValueAsString(
                java.util.Map.of("primaryCode", 11, "chatting", "test"));

        //when, then
        mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("chatting이 빈 문자열이면 400을 반환한다.")
    void recommend_returns400_whenChattingIsBlank() throws Exception {
        //given
        User user = persistUser("KR");
        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());

        //when, then
        mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(11, 10, "   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰이 없으면 401을 반환한다.")
    void recommend_returns401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/symptoms/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(11, 10, "test")))
                .andExpect(status().isUnauthorized());
    }

}
