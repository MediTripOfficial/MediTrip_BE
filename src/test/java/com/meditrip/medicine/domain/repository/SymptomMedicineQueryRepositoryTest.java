package com.meditrip.medicine.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Ingredient;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIngredients;
import com.meditrip.medicine.domain.entity.MedicineSymptomCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SymptomMedicineQueryRepositoryTest {

    @Autowired
    private SymptomMedicineQueryRepository symptomMedicineQueryRepository;

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

    @AfterEach
    void tearDown() {
        medicineRepository.deleteAllInBatch();
        ingredientRepository.deleteAllInBatch();
        medicineIngredientsRepository.deleteAllInBatch();
        medicineSymptomCodeRepository.deleteAllInBatch();
        hashtagRepository.deleteAllInBatch();
        medicineHashtagsRepository.deleteAllInBatch();
    }

    private Medicine persistMedicine(String nameEn) {
        Medicine medicine = Medicine.builder()
                .nameEn(nameEn)
                .manufacturerEn("Test Manufacturer")
                .countryCode("KR")
                .isConvenienceStore(true)
                .severityTier(1)
                .build();
        return medicineRepository.save(medicine);
    }

    private Ingredient persistIngredient(String nameEn) {
        Ingredient ingredient = Ingredient.builder()
                .nameEn(nameEn)
                .nameKo(nameEn)
                .build();
        return ingredientRepository.save(ingredient);
    }

    private Hashtag persistHashtag(String name, HashtagType type) {
        Hashtag hashtag = Hashtag.builder()
                .name(name)
                .type(type)
                .build();
        return hashtagRepository.save(hashtag);
    }

    private void linkMedicineSymptomCode(Long medicineId, Integer symptomCode) {
        medicineSymptomCodeRepository.save(MedicineSymptomCode.builder()
                .medicineId(medicineId)
                .symptomCode(symptomCode)
                .build());
    }

    private void linkMedicineIngredient(Long medicineId, Long ingredientId, String amount) {
        medicineIngredientsRepository.save(MedicineIngredients.builder()
                .medicineId(medicineId)
                .ingredientId(ingredientId)
                .amount(amount)
                .build());
    }

    private void linkMedicineHashtag(Long medicineId, Long hashtagId) {
        medicineHashtagsRepository.save(MedicineHashtags.builder()
                .medicineId(medicineId)
                .hashtagId(hashtagId)
                .build());
    }

    @Test
    @DisplayName("symptomCode에 매핑된 약들을 정확히 조회한다.")
    void findMedicinesBySymptomCode_returnsMatchingMedicines() {
        //given
        Medicine tylenol = persistMedicine("Tylenol");
        Medicine ibuprofen = persistMedicine("Ibuprofen");
        Medicine eyeDrop = persistMedicine("EyeDrop"); // 다른 코드에 매핑됨

        linkMedicineSymptomCode(tylenol.getId(), 11);
        linkMedicineSymptomCode(ibuprofen.getId(), 11);
        linkMedicineSymptomCode(eyeDrop.getId(), 51);

        //when
        List<Medicine> result = symptomMedicineQueryRepository.findMedicinesBySymptomCode(11);

        //then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Medicine::getId)
                .containsExactlyInAnyOrder(tylenol.getId(), ibuprofen.getId());
    }

    @Test
    @DisplayName("symptomCode에 매핑된 약이 하나도 없으면 빈 리스트를 반환한다.")
    void findMedicinesBySymptomCode_returnsEmptyList_whenNoMapping() {
        //when
        List<Medicine> result = symptomMedicineQueryRepository.findMedicinesBySymptomCode(999);

        //then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 약이 같은 symptomCode에 중복으로 매핑되어 있어도 결과는 중복 없이 1번만 나온다.")
    void findMedicinesBySymptomCode_returnsDistinctMedicines_whenDuplicateMapping() {
        //given
        Medicine medicine = persistMedicine("DuplicatedMapping");
        linkMedicineSymptomCode(medicine.getId(), 41);
        linkMedicineSymptomCode(medicine.getId(), 41); // 중복 매핑

        //when
        List<Medicine> result = symptomMedicineQueryRepository.findMedicinesBySymptomCode(41);

        //then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(medicine.getId());
    }

    @Test
    @DisplayName("한 약이 여러 symptomCode에 매핑되어 있으면 각 코드로 조회할 때마다 나타난다.")
    void findMedicinesBySymptomCode_oneMedicineMappedToMultipleCodes() {
        //given
        Medicine medicine = persistMedicine("MultiSymptomMedicine");
        linkMedicineSymptomCode(medicine.getId(), 11);
        linkMedicineSymptomCode(medicine.getId(), 12);

        //when
        List<Medicine> resultFor11 = symptomMedicineQueryRepository.findMedicinesBySymptomCode(11);
        List<Medicine> resultFor12 = symptomMedicineQueryRepository.findMedicinesBySymptomCode(12);

        //then
        assertThat(resultFor11).extracting(Medicine::getId).containsExactly(medicine.getId());
        assertThat(resultFor12).extracting(Medicine::getId).containsExactly(medicine.getId());
    }

    @Test
    @DisplayName("medicineIds가 빈 리스트면 빈 Map을 반환한다. (쿼리 자체를 안 날림)")
    void findIngredientNamesByMedicineIds_returnsEmptyMap_whenIdsEmpty() {
        //when
        Map<Long, List<String>> result = symptomMedicineQueryRepository.findIngredientNamesByMedicineIds(List.of());

        //then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("약 하나에 성분이 여러 개면 리스트로 전부 묶여서 반환된다.")
    void findIngredientNamesByMedicineIds_groupsMultipleIngredientsPerMedicine() {
        //given
        Medicine medicine = persistMedicine("ComboMedicine");
        Ingredient acetaminophen = persistIngredient("Acetaminophen");
        Ingredient caffeine = persistIngredient("Caffeine");

        linkMedicineIngredient(medicine.getId(), acetaminophen.getId(), "300mg");
        linkMedicineIngredient(medicine.getId(), caffeine.getId(), "50mg");

        //when
        Map<Long, List<String>> result = symptomMedicineQueryRepository
                .findIngredientNamesByMedicineIds(List.of(medicine.getId()));

        //then
        assertThat(result).containsKey(medicine.getId());
        assertThat(result.get(medicine.getId())).containsExactlyInAnyOrder("Acetaminophen", "Caffeine");
    }

    @Test
    @DisplayName("요청한 medicineIds에 없는 약의 성분은 결과에 포함되지 않는다.")
    void findIngredientNamesByMedicineIds_doesNotLeakOtherMedicines() {
        //given
        Medicine target = persistMedicine("TargetMedicine");
        Medicine other = persistMedicine("OtherMedicine");
        Ingredient ingredient = persistIngredient("SomeIngredient");

        linkMedicineIngredient(target.getId(), ingredient.getId(), "100mg");
        linkMedicineIngredient(other.getId(), ingredient.getId(), "100mg");

        //when
        Map<Long, List<String>> result = symptomMedicineQueryRepository
                .findIngredientNamesByMedicineIds(List.of(target.getId()));

        //then
        assertThat(result).containsOnlyKeys(target.getId());
        assertThat(result).doesNotContainKey(other.getId());
    }

    @Test
    @DisplayName("medicineIds가 빈 리스트면 빈 리스트를 반환한다.")
    void findDiseaseHashtagNamesByMedicineIds_returnsEmptyList_whenIdsEmpty() {
        //when
        List<String> result = symptomMedicineQueryRepository.findDiseaseHashtagNamesByMedicineIds(List.of());

        //then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DISEASE 타입 해시태그만 반환하고 EFFICACY 타입은 제외한다.")
    void findDiseaseHashtagNamesByMedicineIds_excludesEfficacyType() {
        //given
        Medicine medicine = persistMedicine("HashtagMedicine");
        Hashtag headache = persistHashtag("Headache", HashtagType.DISEASE);
        Hashtag painRelief = persistHashtag("painRelief", HashtagType.EFFICACY);

        linkMedicineHashtag(medicine.getId(), headache.getId());
        linkMedicineHashtag(medicine.getId(), painRelief.getId());

        //when
        List<String> result = symptomMedicineQueryRepository
                .findDiseaseHashtagNamesByMedicineIds(List.of(medicine.getId()));

        //then
        assertThat(result).containsExactly("Headache");
        assertThat(result).doesNotContain("painRelief");
    }

    @Test
    @DisplayName("여러 약이 같은 해시태그를 공유하면 중복 없이 한 번만 나온다.")
    void findDiseaseHashtagNamesByMedicineIds_returnsDistinctNames() {
        //given
        Medicine medicineA = persistMedicine("MedicineA");
        Medicine medicineB = persistMedicine("MedicineB");
        Hashtag fever = persistHashtag("Fever", HashtagType.DISEASE);

        linkMedicineHashtag(medicineA.getId(), fever.getId());
        linkMedicineHashtag(medicineB.getId(), fever.getId());

        //when
        List<String> result = symptomMedicineQueryRepository
                .findDiseaseHashtagNamesByMedicineIds(List.of(medicineA.getId(), medicineB.getId()));

        //then
        assertThat(result).containsExactly("Fever");
    }

}
