package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.application.dto.response.MedicineResponse;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.user.application.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MedicineFacadeGetInfoTest {

    @InjectMocks
    private MedicineFacade medicineFacade;

    @Mock
    private MedicineService medicineService;

    @Mock
    private MedicineReviewService medicineReviewService;

    @Mock
    private UserService userService;

    private static MedicineInfo medicineInfo(Long id) {
        return MedicineInfo.builder()
                .id(id)
                .name("Tylenol")
                .manufacturer("Johnson & Johnson")
                .build();
    }

    private static MedicineReview review(Long id, UUID userId, Double rating) {
        MedicineReview review = MedicineReview.create(
                1L, "약이 정말 좋네요.", 170.0, 60.0, rating, "Female", "KR", userId, "Headache");

        ReflectionTestUtils.setField(review, "createdAt", Instant.now());
        return review;
    }

    @DisplayName("리뷰가 하나도 없는 약을 조회해도 예외 없이 rating=null, reviewCount=0, topReview=null로 반환된다.")
    @Test
    void shouldReturnNullRatingAndZeroCount_whenMedicineHasNoReviews() {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();

        given(medicineService.getInfo(medicineId, userId)).willReturn(medicineInfo(medicineId));
        given(medicineReviewService.getReviews(medicineId)).willReturn(List.of());
        given(medicineService.getMedicineTopReview(medicineId)).willReturn(null);

        //when
        MedicineResponse response = medicineFacade.getInfo(medicineId, userId);

        //then
        assertThat(response.getRating()).isNull();
        assertThat(response.getReviewCount()).isZero();
        assertThat(response.getTopReview()).isNull();

        verify(userService, never()).getReviewUserInfo(any());
    }

    @DisplayName("리뷰가 여러 개면 평균 평점과 개수가 정확히 계산되고, top review에 작성자 닉네임/프로필이 채워진다.")
    @Test
    void shouldComputeAverageRatingAndBuildTopReview_whenReviewsExist() {
        //given
        Long medicineId = 1L;
        UUID requestUserId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        given(medicineService.getInfo(medicineId, requestUserId)).willReturn(medicineInfo(medicineId));

        MedicineReview review1 = review(1L, authorId, 4.0);
        MedicineReview review2 = review(2L, authorId, 5.0);
        given(medicineReviewService.getReviews(medicineId)).willReturn(List.of(review1, review2));

        MedicineReview topReview = review(2L, authorId, 5.0);
        given(medicineService.getMedicineTopReview(medicineId)).willReturn(topReview);

        UserInfo author = UserInfo.builder().userId(authorId).nickname("닉네임").profileImg("https://img.com/a.png").build();
        given(userService.getReviewUserInfo(authorId)).willReturn(author);

        //when
        MedicineResponse response = medicineFacade.getInfo(medicineId, requestUserId);

        //then
        assertThat(response.getRating()).isEqualTo(4.5);
        assertThat(response.getReviewCount()).isEqualTo(2);
        assertThat(response.getTopReview()).isNotNull();
        assertThat(response.getTopReview().getNickname()).isEqualTo("닉네임");
        assertThat(response.getTopReview().getProfileImg()).isEqualTo("https://img.com/a.png");
    }

    @DisplayName("리뷰가 1개면 평균 평점은 그 리뷰의 평점과 같다.")
    @Test
    void shouldReturnSameRating_whenOnlyOneReviewExists() {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        given(medicineService.getInfo(medicineId, userId)).willReturn(medicineInfo(medicineId));
        given(medicineReviewService.getReviews(medicineId)).willReturn(List.of(review(1L, authorId, 3.5)));
        given(medicineService.getMedicineTopReview(medicineId)).willReturn(review(1L, authorId, 3.5));
        UserInfo author = UserInfo.builder().userId(authorId).nickname("닉네임").profileImg("https://img.com/a.png").build();
        given(userService.getReviewUserInfo(authorId)).willReturn(author);

        //when
        MedicineResponse response = medicineFacade.getInfo(medicineId, userId);

        //then
        assertThat(response.getRating()).isEqualTo(3.5);
        assertThat(response.getReviewCount()).isEqualTo(1);
    }

    @DisplayName("top review의 작성자 계정을 찾을 수 없어도(탈퇴 등) 예외 없이 nickname/profileImg가 null인 topReview로 반환된다.")
    @Test
    void shouldReturnTopReviewWithNullAuthorInfo_whenAuthorNotFound() {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();
        UUID withdrawnAuthorId = UUID.randomUUID();

        given(medicineService.getInfo(medicineId, userId)).willReturn(medicineInfo(medicineId));
        given(medicineReviewService.getReviews(medicineId))
                .willReturn(List.of(review(1L, withdrawnAuthorId, 5.0)));

        MedicineReview topReview = review(1L, withdrawnAuthorId, 5.0);
        given(medicineService.getMedicineTopReview(medicineId)).willReturn(topReview);
        given(userService.getReviewUserInfo(withdrawnAuthorId)).willReturn(null);

        //when
        MedicineResponse response = medicineFacade.getInfo(medicineId, userId);

        //then
        assertThat(response.getTopReview()).isNotNull();
        assertThat(response.getTopReview().getNickname()).isEqualTo("탈퇴한 유저");
        assertThat(response.getTopReview().getProfileImg()).isNull();
    }

    @DisplayName("조회할 약이 존재하지 않으면 MedicineNotFoundException이 그대로 전파되고, 리뷰/유저 조회는 수행되지 않는다.")
    @Test
    void shouldPropagateException_whenMedicineDoesNotExist() {
        //given
        Long medicineId = 999L;
        UUID userId = UUID.randomUUID();

        given(medicineService.getInfo(medicineId, userId)).willThrow(new MedicineNotFoundException());

        //when, then
        assertThatThrownBy(() -> medicineFacade.getInfo(medicineId, userId))
                .isInstanceOf(MedicineNotFoundException.class);

        verify(medicineReviewService, never()).getReviews(any());
        verify(medicineService, never()).getMedicineTopReview(any());
        verify(userService, never()).getReviewUserInfo(any());
    }

    @DisplayName("getReviews와 getMedicineTopReview는 각각 정확히 한 번씩만 호출된다.")
    @Test
    void shouldCallReviewQueriesExactlyOnce() {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();

        given(medicineService.getInfo(medicineId, userId)).willReturn(medicineInfo(medicineId));
        given(medicineReviewService.getReviews(medicineId)).willReturn(List.of());
        given(medicineService.getMedicineTopReview(medicineId)).willReturn(null);

        //when
        medicineFacade.getInfo(medicineId, userId);

        //then
        verify(medicineReviewService, times(1)).getReviews(medicineId);
        verify(medicineService, times(1)).getMedicineTopReview(medicineId);
    }

}
