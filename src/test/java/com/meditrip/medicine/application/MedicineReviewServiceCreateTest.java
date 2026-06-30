package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.user.domain.entity.enums.Gender;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicineReviewServiceCreateTest {

    @InjectMocks
    private MedicineReviewService medicineReviewService;

    @Mock
    private MedicineReviewRepository medicineReviewRepository;

    @DisplayName("약 리뷰 생성에 성공한다.")
    @Test
    void shouldCreateMedicineReviewSuccessfully() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        UserInfo userInfo = UserInfo.builder()
                .userId(userId)
                .gender(Gender.F.name())
                .country("US")
                .weight(103.0)
                .height(190.0)
                .age(12)
                .build();

        CreateMedicineReviewApplicationRequest request = CreateMedicineReviewApplicationRequest.builder()
                .symptom("symptom")
                .review("약이 정말 좋네요.")
                .rating(5.0)
                .build();

        MedicineReview mockSavedReview = mock(MedicineReview.class);
        given(mockSavedReview.getId()).willReturn(1L);
        given(medicineReviewRepository.save(any(MedicineReview.class))).willReturn(mockSavedReview);

        //when
        Long reviewId = medicineReviewService.create(userInfo, medicineId, request);

        //then
        assertThat(reviewId).isEqualTo(1L);
        verify(medicineReviewRepository, times(1)).save(any(MedicineReview.class));
    }

}
