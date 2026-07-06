package com.meditrip.medicine.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.meditrip.medicine.domain.ReviewSortType;
import com.meditrip.medicine.domain.entity.MedicineReview;
import java.util.List;
import java.util.UUID;
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
class MedicineReviewQueryRepositoryTest {

    @Autowired
    private MedicineReviewQueryRepository medicineReviewQueryRepository;

    @Autowired
    private MedicineReviewRepository medicineReviewRepository;

    @AfterEach
    void tearDown() {
        medicineReviewRepository.deleteAllInBatch();
    }

    private MedicineReview persistReview(Long medicineId, Double rating, String gender, String country,
                                         String symptom) {
        MedicineReview review = MedicineReview.create(
                medicineId, "리뷰 내용", 170.0, 60.0, rating, gender, country, UUID.randomUUID(), symptom, null);
        return medicineReviewRepository.save(review);
    }

    @DisplayName("medicineId가 일치하는 리뷰만 조회한다.")
    @Test
    void shouldReturnOnlyMatchingMedicineReviews() {
        //given
        Long medicineId = 1L;

        MedicineReview matched = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(2L, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(matched.getId());
    }

    @DisplayName("삭제된(isDeleted=true) 리뷰는 조회 결과에서 제외된다.")
    @Test
    void shouldExcludeDeletedReviews() {
        //given
        Long medicineId = 1L;

        MedicineReview activeReview = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        MedicineReview deletedReview = persistReview(medicineId, 4.0, "Male", "KR", "Headache");
        deletedReview.delete();
        medicineReviewRepository.save(deletedReview);

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(activeReview.getId())
                .doesNotContain(deletedReview.getId());
    }

    @DisplayName("같은 약에 삭제된 리뷰와 삭제되지 않은 리뷰가 섞여 있어도, 삭제되지 않은 리뷰의 개수만 정확히 조회된다.")
    @Test
    void shouldReturnOnlyNonDeletedReviewsCount_whenMixedWithDeletedReviews() {
        //given
        Long medicineId = 1L;

        persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(medicineId, 4.0, "Female", "KR", "Headache");

        MedicineReview deleted1 = persistReview(medicineId, 3.0, "Male", "KR", "Headache");
        deleted1.delete();
        medicineReviewRepository.save(deleted1);

        MedicineReview deleted2 = persistReview(medicineId, 2.0, "Male", "KR", "Headache");
        deleted2.delete();
        medicineReviewRepository.save(deleted2);

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, null, 10, ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).hasSize(2);
    }

    @DisplayName("sort가 LATEST이면 id 내림차순(최신순)으로 정렬되어 조회된다.")
    @Test
    void shouldReturnReviewsOrderedByIdDescending_whenSortIsLatest() {
        //given
        Long medicineId = 1L;

        MedicineReview first = persistReview(medicineId, 3.0, "Female", "KR", "Headache");
        MedicineReview second = persistReview(medicineId, 4.0, "Male", "US", "Fever");
        MedicineReview third = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @DisplayName("sort가 HIGHEST_RATING이면 평점 내림차순, 동점이면 id 내림차순으로 정렬되어 조회된다.")
    @Test
    void shouldReturnReviewsOrderedByRatingDescending_whenSortIsHighestRating() {
        //given
        Long medicineId = 1L;

        MedicineReview low = persistReview(medicineId, 2.0, "Female", "KR", "Headache");
        MedicineReview highFirst = persistReview(medicineId, 5.0, "Male", "US", "Fever");
        MedicineReview highSecond = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.HIGHEST_RATING, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId)
                .containsExactly(highSecond.getId(), highFirst.getId(), low.getId());
    }

    @DisplayName("sort가 LOWEST_RATING이면 평점 오름차순, 동점이면 id 내림차순으로 정렬되어 조회된다.")
    @Test
    void shouldReturnReviewsOrderedByRatingAscending_whenSortIsLowestRating() {
        //given
        Long medicineId = 1L;

        MedicineReview high = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview lowFirst = persistReview(medicineId, 1.0, "Male", "US", "Fever");
        MedicineReview lowSecond = persistReview(medicineId, 1.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LOWEST_RATING, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId)
                .containsExactly(lowSecond.getId(), lowFirst.getId(), high.getId());
    }

    @DisplayName("gender 필터를 지정하면 해당 gender의 리뷰만 조회된다.")
    @Test
    void shouldFilterReviewsByGender() {
        //given
        Long medicineId = 1L;

        MedicineReview femaleReview = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(medicineId, 5.0, "Male", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, List.of("Female"), null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(femaleReview.getId());
    }

    @DisplayName("gender 필터에 여러 값을 지정하면 OR 조건으로 조회된다.")
    @Test
    void shouldFilterReviewsByMultipleGenders() {
        //given
        Long medicineId = 1L;

        MedicineReview femaleReview = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview maleReview = persistReview(medicineId, 5.0, "Male", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, List.of("Female", "Male"), null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId)
                .containsExactlyInAnyOrder(femaleReview.getId(), maleReview.getId());
    }

    @DisplayName("countries 필터를 지정하면 해당 국가의 리뷰만 조회된다.")
    @Test
    void shouldFilterReviewsByCountries() {
        //given
        Long medicineId = 1L;

        MedicineReview krReview = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(medicineId, 5.0, "Female", "US", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, null, List.of("KR"), null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(krReview.getId());
    }

    @DisplayName("symptoms 필터를 지정하면 해당 증상의 리뷰만 조회된다.")
    @Test
    void shouldFilterReviewsBySymptoms() {
        //given
        Long medicineId = 1L;

        MedicineReview headacheReview = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(medicineId, 5.0, "Female", "KR", "Fever");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, null, null, List.of("Headache"));

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(headacheReview.getId());
    }

    @DisplayName("gender, countries, symptoms 필터를 동시에 지정하면 모두 만족하는 리뷰만 조회된다(AND 조건).")
    @Test
    void shouldFilterReviewsByAllFiltersTogether() {
        //given
        Long medicineId = 1L;

        MedicineReview matched = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(medicineId, 5.0, "Male", "KR", "Headache"); // gender 불일치
        persistReview(medicineId, 5.0, "Female", "US", "Headache"); // country 불일치
        persistReview(medicineId, 5.0, "Female", "KR", "Fever"); // symptom 불일치

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, List.of("Female"), List.of("KR"), List.of("Headache"));

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(matched.getId());
    }

    @DisplayName("필터에 빈 리스트를 전달하면 필터링하지 않고 전체를 조회한다.")
    @Test
    void shouldNotFilter_whenFilterListsAreEmpty() {
        //given
        Long medicineId = 1L;

        MedicineReview review = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 10,
                ReviewSortType.LATEST, List.of(), List.of(), List.of());

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(review.getId());
    }

    @DisplayName("cursor가 null이면 처음부터 limit만큼 조회한다.")
    @Test
    void shouldReturnFromBeginning_whenCursorIsNull() {
        //given
        Long medicineId = 1L;

        for (int i = 0; i < 5; i++) {
            persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        }

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(medicineId, null, 3,
                ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).hasSize(3);
    }

    @DisplayName("sort가 LATEST일 때 cursor를 지정하면 해당 id보다 작은 리뷰만 조회된다.")
    @Test
    void shouldReturnReviewsWithSmallerIdThanCursor_whenSortIsLatest() {
        //given
        Long medicineId = 1L;

        MedicineReview first = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview second = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview third = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, third.getId(), 10, ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @DisplayName("sort가 HIGHEST_RATING일 때 cursor를 지정하면 (rating, id) 복합 키 기준으로 그 다음 리뷰만 조회된다.")
    @Test
    void shouldReturnNextReviewsByCompositeKey_whenSortIsHighestRatingWithCursor() {
        //given
        Long medicineId = 1L;

        MedicineReview first = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview second = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview third = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, third.getId(), 10, ReviewSortType.HIGHEST_RATING, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(second.getId(), first.getId());
    }

    @DisplayName("rating이 다른 리뷰들 사이에서 HIGHEST_RATING cursor 조회 시 더 낮은 rating의 리뷰까지 포함하여 조회된다.")
    @Test
    void shouldIncludeLowerRatingReviews_whenCursorRatingDiffersOnHighestRating() {
        //given
        Long medicineId = 1L;

        MedicineReview highRating = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview midRating = persistReview(medicineId, 3.0, "Female", "KR", "Headache");
        MedicineReview lowRating = persistReview(medicineId, 1.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, highRating.getId(), 10, ReviewSortType.HIGHEST_RATING, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(midRating.getId(), lowRating.getId());
    }

    @DisplayName("sort가 LOWEST_RATING일 때 cursor를 지정하면 더 높은 rating의 리뷰까지 포함하여 조회된다.")
    @Test
    void shouldIncludeHigherRatingReviews_whenCursorRatingDiffersOnLowestRating() {
        //given
        Long medicineId = 1L;

        MedicineReview lowRating = persistReview(medicineId, 1.0, "Female", "KR", "Headache");
        MedicineReview midRating = persistReview(medicineId, 3.0, "Female", "KR", "Headache");
        MedicineReview highRating = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, lowRating.getId(), 10, ReviewSortType.LOWEST_RATING, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactly(midRating.getId(), highRating.getId());
    }

    @DisplayName("cursor로 전달된 id가 존재하지 않으면 id 기준으로만 안전하게 필터링하여 조회된다.")
    @Test
    void shouldFallbackToIdFiltering_whenCursorIdDoesNotExist() {
        //given
        Long medicineId = 1L;

        MedicineReview first = persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        MedicineReview second = persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        Long nonExistentCursor = second.getId() + 999L;

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, nonExistentCursor, 10, ReviewSortType.HIGHEST_RATING, null, null, null);

        //then
        assertThat(result).extracting(MedicineReview::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @DisplayName("limit보다 조회 대상 리뷰가 적으면 있는 만큼만 조회된다.")
    @Test
    void shouldReturnFewerThanLimit_whenNotEnoughReviewsExist() {
        //given
        Long medicineId = 1L;

        persistReview(medicineId, 5.0, "Female", "KR", "Headache");
        persistReview(medicineId, 5.0, "Female", "KR", "Headache");

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, null, 10, ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).hasSize(2);
    }

    @DisplayName("조건에 맞는 리뷰가 하나도 없으면 빈 리스트를 반환한다.")
    @Test
    void shouldReturnEmptyList_whenNoReviewMatches() {
        //given
        Long medicineId = 999L;

        //when
        List<MedicineReview> result = medicineReviewQueryRepository.findReviews(
                medicineId, null, 10, ReviewSortType.LATEST, null, null, null);

        //then
        assertThat(result).isEmpty();
    }

}
