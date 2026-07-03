package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.response.CursorResponse;
import com.meditrip.medicine.application.dto.ReviewAuthorInfo;
import com.meditrip.medicine.application.dto.request.GetMedicineReviewsApplicationRequest;
import com.meditrip.medicine.application.dto.response.MedicineReviewsResponse;
import com.meditrip.medicine.domain.ReviewSortType;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineReviewQueryRepository;
import com.meditrip.user.application.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MedicineReviewFacadeGetReviewsTest {

    @InjectMocks
    private MedicineReviewFacade medicineReviewFacade;

    @Mock
    private UserService userService;

    @Mock
    private MedicineReviewService medicineReviewService;

    @Mock
    private MedicineService medicineService;

    @Mock
    private MedicineReviewQueryRepository medicineReviewQueryRepository;

    private static MedicineReview review(Long id, UUID userId) {
        MedicineReview review = MedicineReview.create(
                1L, "약이 정말 좋네요.", 170.0, 60.0, 5.0, "Female", "KR", userId, "Headache");
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "createdAt", Instant.now());
        return review;
    }

    @DisplayName("약 리뷰 조회에 성공하고, 조회된 리뷰 수만큼 닉네임이 매핑되어 응답된다.")
    @Test
    void shouldGetReviewsSuccessfully_whenMedicineExists() {
        //given
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .cursor(null)
                .size(15)
                .sort(ReviewSortType.LATEST)
                .gender(null)
                .countries(null)
                .symptoms(null)
                .build();

        MedicineReview review = review(1L, authorId);

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 16, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of(review));

        ReviewAuthorInfo reviewAuthorInfo = ReviewAuthorInfo.builder()
                .age(28)
                .profileImg("https://profile.img/qwer")
                .nickname("닉네임")
                .build();
        given(userService.getReviewAuthorInfoByUserIds(List.of(authorId))).willReturn(Map.of(authorId, reviewAuthorInfo));

        //when
        CursorResponse<MedicineReviewsResponse> response = medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getNickname()).isEqualTo("닉네임");
        assertThat(response.getItems().get(0).getAuthorAgeGroup()).isEqualTo("20s");
        assertThat(response.getItems().get(0).getUserProfileImg()).isEqualTo("https://profile.img/qwer");
        assertThat(response.getItems().get(0).isAuthor()).isFalse();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();

        verify(medicineService, times(1)).existsById(medicineId);
        verify(medicineReviewQueryRepository, times(1))
                .findReviews(medicineId, null, 16, ReviewSortType.LATEST, null, null, null);
        verify(userService, times(1)).getReviewAuthorInfoByUserIds(List.of(authorId));
    }

    @DisplayName("조회한 리뷰가 본인이 작성한 리뷰면 isAuthor가 true로 응답된다.")
    @Test
    void shouldMarkIsAuthorTrue_whenReviewBelongsToRequestUser() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(15)
                .sort(ReviewSortType.LATEST)
                .build();

        MedicineReview ownReview = review(1L, userId);

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 16, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of(ownReview));

        ReviewAuthorInfo reviewAuthorInfo = ReviewAuthorInfo.builder()
                .age(28)
                .profileImg("https://profile.img/qwer")
                .nickname("닉네임")
                .build();
        given(userService.getReviewAuthorInfoByUserIds(List.of(userId))).willReturn(Map.of(userId, reviewAuthorInfo));

        //when
        CursorResponse<MedicineReviewsResponse> response = medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        assertThat(response.getItems().get(0).isAuthor()).isTrue();
    }

    @DisplayName("size가 null이면 기본값 15가 적용되어 조회된다.")
    @Test
    void shouldUseDefaultSize_whenSizeIsNull() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(null)
                .sort(ReviewSortType.LATEST)
                .build();

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 16, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of());
        given(userService.getReviewAuthorInfoByUserIds(List.of())).willReturn(Map.of());

        //when
        medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        verify(medicineReviewQueryRepository, times(1)).findReviews(medicineId, null, 16, ReviewSortType.LATEST, null,
                null, null);
    }

    @DisplayName("조회된 리뷰 개수가 요청 size보다 많으면 hasNext가 true이고, nextCursor는 마지막 페이지 항목의 id다.")
    @Test
    void shouldReturnHasNextTrue_whenReviewCountExceedsRequestedSize() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(2)
                .sort(ReviewSortType.LATEST)
                .build();

        UUID authorId = UUID.randomUUID();
        MedicineReview first = review(3L, authorId);
        MedicineReview second = review(2L, authorId);
        MedicineReview third = review(1L, authorId);

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 3, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of(first, second, third));

        ReviewAuthorInfo reviewAuthorInfo = ReviewAuthorInfo.builder()
                .age(28)
                .profileImg("https://profile.img/qwer")
                .nickname("닉네임")
                .build();
        given(userService.getReviewAuthorInfoByUserIds(List.of(authorId))).willReturn(Map.of(authorId, reviewAuthorInfo));

        //when
        CursorResponse<MedicineReviewsResponse> response = medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(String.valueOf(second.getId()));
    }

    @DisplayName("조회된 리뷰 개수가 요청 size 이하이면 hasNext가 false이고, nextCursor는 null이다.")
    @Test
    void shouldReturnHasNextFalse_whenReviewCountDoesNotExceedRequestedSize() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(2)
                .sort(ReviewSortType.LATEST)
                .build();

        UUID authorId = UUID.randomUUID();
        MedicineReview onlyReview = review(1L, authorId);

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 3, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of(onlyReview));

        ReviewAuthorInfo reviewAuthorInfo = ReviewAuthorInfo.builder()
                .age(28)
                .profileImg("https://profile.img/qwer")
                .nickname("닉네임")
                .build();
        given(userService.getReviewAuthorInfoByUserIds(List.of(authorId))).willReturn(Map.of(authorId, reviewAuthorInfo));

        //when
        CursorResponse<MedicineReviewsResponse> response = medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @DisplayName("같은 작성자가 여러 리뷰를 작성했어도 닉네임 조회는 distinct된 userId로 한 번만 요청한다.")
    @Test
    void shouldRequestNicknamesWithDistinctUserIds_whenSameAuthorWroteMultipleReviews() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;
        UUID authorId = UUID.randomUUID();

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(15)
                .sort(ReviewSortType.LATEST)
                .build();

        MedicineReview firstReview = review(2L, authorId);
        MedicineReview secondReview = review(1L, authorId);

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 16, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of(firstReview, secondReview));

        ReviewAuthorInfo reviewAuthorInfo = ReviewAuthorInfo.builder()
                .age(28)
                .profileImg("https://profile.img/qwer")
                .nickname("닉네임")
                .build();
        given(userService.getReviewAuthorInfoByUserIds(List.of(authorId))).willReturn(Map.of(authorId, reviewAuthorInfo));

        //when
        CursorResponse<MedicineReviewsResponse> response = medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        assertThat(response.getItems()).hasSize(2);
        verify(userService, times(1)).getReviewAuthorInfoByUserIds(List.of(authorId));
    }

    @DisplayName("조회된 리뷰가 없으면 빈 목록과 hasNext false, nextCursor null을 반환하고, 닉네임 조회는 빈 리스트로 호출된다.")
    @Test
    void shouldReturnEmptyResult_whenNoReviewsFound() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(15)
                .sort(ReviewSortType.LATEST)
                .build();

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, null, 16, ReviewSortType.LATEST, null, null,
                null)).willReturn(List.of());
        given(userService.getReviewAuthorInfoByUserIds(List.of())).willReturn(Map.of());

        //when
        CursorResponse<MedicineReviewsResponse> response = medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @DisplayName("조회할 약이 존재하지 않으면 MedicineNotFoundException이 발생하고, 리뷰 조회는 수행되지 않는다.")
    @Test
    void shouldThrowException_whenMedicineDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .size(15)
                .sort(ReviewSortType.LATEST)
                .build();

        given(medicineService.existsById(medicineId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> medicineReviewFacade.getReviews(userId, medicineId, request))
                .isInstanceOf(MedicineNotFoundException.class)
                .hasMessage("Medicine Not Found");

        verify(medicineService, times(1)).existsById(medicineId);
        verify(medicineReviewQueryRepository, never()).findReviews(any(), any(), anyInt(), any(), any(), any(), any());
        verify(userService, never()).getReviewAuthorInfoByUserIds(anyList());
    }

    @DisplayName("필터(gender, countries, symptoms)를 그대로 쿼리 레포지토리에 전달한다.")
    @Test
    void shouldPassFiltersToQueryRepositoryAsIs() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        List<String> genders = List.of("Female", "Male");
        List<String> countries = List.of("KR", "US");
        List<String> symptoms = List.of("Headache", "Fever");

        GetMedicineReviewsApplicationRequest request = GetMedicineReviewsApplicationRequest.builder()
                .medicineId(medicineId)
                .cursor(100L)
                .size(10)
                .sort(ReviewSortType.HIGHEST_RATING)
                .gender(genders)
                .countries(countries)
                .symptoms(symptoms)
                .build();

        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewQueryRepository.findReviews(medicineId, 100L, 11, ReviewSortType.HIGHEST_RATING, genders,
                countries, symptoms)).willReturn(List.of());
        given(userService.getReviewAuthorInfoByUserIds(List.of())).willReturn(Map.of());

        //when
        medicineReviewFacade.getReviews(userId, medicineId, request);

        //then
        verify(medicineReviewQueryRepository, times(1))
                .findReviews(medicineId, 100L, 11, ReviewSortType.HIGHEST_RATING, genders, countries, symptoms);
    }

}
