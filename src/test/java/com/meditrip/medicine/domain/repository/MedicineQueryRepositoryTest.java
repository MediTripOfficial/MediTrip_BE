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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MedicineQueryRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    private MedicineQueryRepository medicineQueryRepository;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        medicineQueryRepository = new MedicineQueryRepository(queryFactory);
    }

    @DisplayName("편의점 판매 약은 ingredient, 해시태그, purchaseLocation이 모두 매핑되어 조회된다.")
    @Test
    void shouldReturnFullMedicineInfo_whenMedicineIsSoldAtConvenienceStore() {
        //given
        Medicine medicine = Medicine.builder()
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
                .usageDetailKo("물과 함께 복용")
                .drugInteractionsEn("None known")
                .drugInteractionsKo("알려진 상호작용 없음")
                .seeDoctorEn("If symptoms persist over 3 days")
                .seeDoctorKo("3일 이상 지속되면 의사와 상담")
                .imageUrl("https://example.com/tylenol.png")
                .countryCode("US")
                .build();
        entityManager.persist(medicine); // IDENTITY 전략이라 persist 시점에 id가 채워짐

        Ingredient ingredient = Ingredient.builder()
                .nameEn("Acetaminophen")
                .nameKo("아세트아미노펜")
                .build();
        entityManager.persist(ingredient);

        entityManager.persist(MedicineIngredients.builder()
                .medicineId(medicine.getId())
                .ingredientId(ingredient.getId())
                .amount("500mg")
                .build());

        Hashtag diseaseHashtag = Hashtag.builder()
                .name("headache")
                .type(HashtagType.DISEASE)
                .build();
        entityManager.persist(diseaseHashtag);

        Hashtag efficacyHashtag = Hashtag.builder()
                .name("painRelief")
                .type(HashtagType.EFFICACY)
                .build();
        entityManager.persist(efficacyHashtag);

        entityManager.persist(MedicineHashtags.builder()
                .medicineId(medicine.getId())
                .hashtagId(diseaseHashtag.getId())
                .build());

        entityManager.persist(MedicineHashtags.builder()
                .medicineId(medicine.getId())
                .hashtagId(efficacyHashtag.getId())
                .build());

        entityManager.flush();
        entityManager.clear();

        //when
        Optional<MedicineInfo> result = medicineQueryRepository.findInfoById(medicine.getId());

        //then
        assertThat(result).isPresent();
        MedicineInfo info = result.get();

        assertThat(info.getId()).isEqualTo(medicine.getId());
        assertThat(info.getName()).isEqualTo("Tylenol");
        assertThat(info.getManufacturer()).isEqualTo("Johnson & Johnson");

        assertThat(info.getIngredients()).hasSize(1);
        assertThat(info.getIngredients().get(0).getIngredientName()).isEqualTo("Acetaminophen");
        assertThat(info.getIngredients().get(0).getAmount()).isEqualTo("500mg");

        assertThat(info.getDiseaseHashtags()).containsExactly("headache");
        assertThat(info.getEfficacyHashtags()).containsExactly("painRelief");

        assertThat(info.getIsConvenienceStore()).isTrue();
        assertThat(info.getPurchaseLocation()).containsExactlyInAnyOrder("store", "pharmacy");

        assertThat(info.getRating()).isNull();
        assertThat(info.getReviewCount()).isNull();
        assertThat(info.getTopReview()).isNull();
    }

    @DisplayName("편의점에서 판매하지 않는 약은 purchaseLocation에 pharmacy만 포함된다.")
    @Test
    void shouldReturnPharmacyOnly_whenMedicineIsNotSoldAtConvenienceStore() {
        //given
        Medicine medicine = Medicine.builder()
                .nameKo("처방약")
                .nameEn("Prescription Drug")
                .manufacturerKo("제약사")
                .manufacturerEn("Pharma Co")
                .isConvenienceStore(false)
                .isChildSafe(false)
                .countryCode("US")
                .build();
        entityManager.persist(medicine);
        entityManager.flush();
        entityManager.clear();

        //when
        Optional<MedicineInfo> result = medicineQueryRepository.findInfoById(medicine.getId());

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getPurchaseLocation()).containsExactly("pharmacy");
        assertThat(result.get().getIngredients()).isEmpty();
        assertThat(result.get().getDiseaseHashtags()).isEmpty();
        assertThat(result.get().getEfficacyHashtags()).isEmpty();
    }

    @DisplayName("isConvenienceStore가 null이어도 NPE 없이 pharmacy만 포함된다.")
    @Test
    void shouldReturnPharmacyOnly_whenIsConvenienceStoreIsNull() {
        //given
        Medicine medicine = Medicine.builder()
                .nameEn("Unknown Drug")
                .manufacturerEn("Unknown Co")
                .isConvenienceStore(null)
                .countryCode("US")
                .build();
        entityManager.persist(medicine);
        entityManager.flush();
        entityManager.clear();

        //when
        Optional<MedicineInfo> result = medicineQueryRepository.findInfoById(medicine.getId());

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getPurchaseLocation()).containsExactly("pharmacy");
    }

    @DisplayName("존재하지 않는 약 ID로 조회하면 빈 Optional을 반환한다.")
    @Test
    void shouldReturnEmptyOptional_whenMedicineDoesNotExist() {
        //when
        Optional<MedicineInfo> result = medicineQueryRepository.findInfoById(999_999L);

        //then
        assertThat(result).isEmpty();
    }

}
