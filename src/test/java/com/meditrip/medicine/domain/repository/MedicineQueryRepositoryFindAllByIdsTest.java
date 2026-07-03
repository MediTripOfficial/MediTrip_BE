package com.meditrip.medicine.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Ingredient;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIngredients;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MedicineQueryRepositoryFindAllByIdsTest {

    @PersistenceContext
    private EntityManager entityManager;

    private MedicineQueryRepository medicineQueryRepository;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        medicineQueryRepository = new MedicineQueryRepository(queryFactory);
    }

    private Medicine persistMedicine(String nameEn, boolean isConvenienceStore) {
        Medicine medicine = Medicine.builder()
                .nameKo("약")
                .nameEn(nameEn)
                .manufacturerEn("Pharma Co")
                .isConvenienceStore(isConvenienceStore)
                .isChildSafe(true)
                .countryCode("US")
                .build();
        entityManager.persist(medicine);
        return medicine;
    }

    @DisplayName("medicineId 목록으로 조회하면 각 약의 성분, 해시태그가 매핑된 Map이 반환된다.")
    @Test
    void shouldReturnMedicineInfoMap_whenMedicineIdsProvided() {
        //given
        Medicine medicine1 = persistMedicine("Tylenol", true);
        Medicine medicine2 = persistMedicine("Advil", false);

        Ingredient ingredient = Ingredient.builder()
                .nameEn("Acetaminophen")
                .nameKo("아세트아미노펜")
                .build();
        entityManager.persist(ingredient);

        entityManager.persist(MedicineIngredients.builder()
                .medicineId(medicine1.getId())
                .ingredientId(ingredient.getId())
                .amount("500mg")
                .build());

        Hashtag diseaseHashtag = Hashtag.builder()
                .name("headache")
                .type(HashtagType.DISEASE)
                .build();
        entityManager.persist(diseaseHashtag);

        entityManager.persist(MedicineHashtags.builder()
                .medicineId(medicine1.getId())
                .hashtagId(diseaseHashtag.getId())
                .build());

        entityManager.flush();
        entityManager.clear();

        //when
        Map<Long, MedicineInfo> result =
                medicineQueryRepository.findAllByIds(List.of(medicine1.getId(), medicine2.getId()));

        //then
        assertThat(result).hasSize(2);

        MedicineInfo info1 = result.get(medicine1.getId());
        assertThat(info1.getName()).isEqualTo("Tylenol");
        assertThat(info1.getIngredients()).hasSize(1);
        assertThat(info1.getIngredients().get(0).getIngredientName()).isEqualTo("Acetaminophen");
        assertThat(info1.getDiseaseHashtags()).containsExactly("headache");
        assertThat(info1.getPurchaseLocation()).containsExactlyInAnyOrder("store", "pharmacy");

        MedicineInfo info2 = result.get(medicine2.getId());
        assertThat(info2.getName()).isEqualTo("Advil");
        assertThat(info2.getIngredients()).isEmpty();
        assertThat(info2.getPurchaseLocation()).containsExactly("pharmacy");
    }

    @DisplayName("빈 medicineId 목록으로 조회하면 null이 아닌 빈 Map을 반환한다.")
    @Test
    void shouldReturnEmptyMap_whenMedicineIdsIsEmpty() {
        //when
        Map<Long, MedicineInfo> result = medicineQueryRepository.findAllByIds(List.of());

        //then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @DisplayName("존재하지 않는 medicineId가 섞여 있어도 예외 없이 존재하는 약 정보만 반환된다.")
    @Test
    void shouldReturnOnlyExistingMedicines_whenSomeIdsDoNotExist() {
        //given
        Medicine medicine = persistMedicine("Tylenol", true);
        entityManager.flush();
        entityManager.clear();

        //when
        Map<Long, MedicineInfo> result =
                medicineQueryRepository.findAllByIds(List.of(medicine.getId(), 999999L));

        //then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(medicine.getId());
    }

}
