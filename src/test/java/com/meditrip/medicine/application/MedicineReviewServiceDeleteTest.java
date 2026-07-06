package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MedicineReviewServiceDeleteTest {

    @InjectMocks
    private MedicineReviewService medicineReviewService;

    @Mock
    private MedicineReviewRepository medicineReviewRepository;

    private static MedicineReview activeReview(UUID authorId) {
        return MedicineReview.create(
                1L, "약이 정말 좋네요.", 170.0, 60.0, 5.0, "Female", "KR", authorId, "Headache", null);
    }

    @DisplayName("본인이 작성한 리뷰를 삭제하면 isDeleted가 true로 바뀐다.")
    @Test
    void shouldDeleteReviewSuccessfully_whenRequestUserIsAuthor() {
        //given
        UUID userId = UUID.randomUUID();
        Long reviewId = 1L;

        MedicineReview review = activeReview(userId);

        given(medicineReviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        //when
        medicineReviewService.delete(userId, reviewId);

        //then
        assertThat(review.getIsDeleted()).isTrue();
        verify(medicineReviewRepository, times(1)).findById(reviewId);
    }

    @DisplayName("삭제할 리뷰가 존재하지 않으면 MedicineNotFoundException이 발생한다.")
    @Test
    void shouldThrowException_whenReviewDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();
        Long reviewId = 999L;

        given(medicineReviewRepository.findById(reviewId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> medicineReviewService.delete(userId, reviewId))
                .isInstanceOf(MedicineNotFoundException.class)
                .hasMessage("Review Not Found.");

        verify(medicineReviewRepository, times(1)).findById(reviewId);
    }

    @DisplayName("이미 삭제된 리뷰를 다시 삭제 요청하면 MedicineNotFoundException이 발생한다.")
    @Test
    void shouldThrowException_whenReviewAlreadyDeleted() {
        //given
        UUID userId = UUID.randomUUID();
        Long reviewId = 1L;

        MedicineReview alreadyDeletedReview = activeReview(userId);
        alreadyDeletedReview.delete();

        given(medicineReviewRepository.findById(reviewId)).willReturn(Optional.of(alreadyDeletedReview));

        //when, then
        assertThatThrownBy(() -> medicineReviewService.delete(userId, reviewId))
                .isInstanceOf(MedicineNotFoundException.class)
                .hasMessage("Review Not Found.");
    }

    @DisplayName("본인이 작성하지 않은 리뷰를 삭제하려 하면 AccessDeniedException이 발생하고, 리뷰는 삭제되지 않는다.")
    @Test
    void shouldThrowAccessDeniedException_whenRequestUserIsNotAuthor() {
        //given
        UUID authorId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        Long reviewId = 1L;

        MedicineReview review = activeReview(authorId);

        given(medicineReviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        //when, then
        assertThatThrownBy(() -> medicineReviewService.delete(requestUserId, reviewId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to perform this action.");

        assertThat(review.getIsDeleted()).isFalse();
    }

}
