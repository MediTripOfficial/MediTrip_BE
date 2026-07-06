package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MedicineReviewV1controllerDeleteTest extends ControllerTestSupport {

    @Autowired
    private MedicineReviewRepository medicineReviewRepository;

    @AfterEach
    void tearDown() {
        medicineReviewRepository.deleteAllInBatch();
    }

    private MedicineReview persistReview(UUID authorId) {
        MedicineReview review = MedicineReview.create(1L, "약이 정말 좋네요.", 170.0, 60.0, 5.0, "Female", "KR", authorId,
                "Headache", null);
        return medicineReviewRepository.save(review);
    }

    @DisplayName("본인이 작성한 리뷰를 삭제하면 204를 반환하고, isDeleted가 true로 바뀐다.")
    @Test
    void shouldDeleteReviewSuccessfully_whenRequestUserIsAuthor() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        MedicineReview review = persistReview(userId);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(delete("/api/v1/medicines/reviews/" + review.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isNoContent());

        MedicineReview deletedReview = medicineReviewRepository.findById(review.getId()).orElseThrow();
        assertThat(deletedReview.getIsDeleted()).isTrue();
    }

    @DisplayName("삭제할 리뷰가 존재하지 않으면 404를 반환하고, 삭제에 실패한다.")
    @Test
    void shouldReturn404_whenReviewDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when, then
        mockMvc.perform(delete("/api/v1/medicines/reviews/" + 999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review Not Found."));
    }

    @DisplayName("이미 삭제된 리뷰를 다시 삭제 요청하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenReviewAlreadyDeleted() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        MedicineReview review = persistReview(userId);
        review.delete();
        medicineReviewRepository.save(review);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when, then
        mockMvc.perform(delete("/api/v1/medicines/reviews/" + review.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Review Not Found."));
    }

    @DisplayName("본인이 작성하지 않은 리뷰를 삭제하려 하면 403을 반환하고, 리뷰는 삭제되지 않는다.")
    @Test
    void shouldReturn403_whenRequestUserIsNotAuthor() throws Exception {
        //given
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        MedicineReview review = persistReview(authorId);

        String accessToken = jwtProvider.generateAccessToken(otherUserId.toString());

        //when
        mockMvc.perform(delete("/api/v1/medicines/reviews/" + review.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));

        MedicineReview untouchedReview = medicineReviewRepository.findById(review.getId()).orElseThrow();
        assertThat(untouchedReview.getIsDeleted()).isFalse();
    }

    @DisplayName("토큰 없이 요청하면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsMissing() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        MedicineReview review = persistReview(userId);

        //when, then
        mockMvc.perform(delete("/api/v1/medicines/reviews/" + review.getId()))
                .andExpect(status().isUnauthorized());
    }

}
