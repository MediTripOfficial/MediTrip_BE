package com.meditrip.medicine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.medicine.application.dto.request.CreateMedicineReviewApplicationRequest;
import com.meditrip.medicine.domain.UserInfo;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.user.application.UserService;
import com.meditrip.user.domain.entity.enums.Gender;
import com.meditrip.user.domain.exception.UserNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicineReviewFacadeCreateTest {

    @InjectMocks
    private MedicineReviewFacade medicineReviewFacade;

    @Mock
    private UserService userService;

    @Mock
    private MedicineReviewService medicineReviewService;

    @Mock
    private MedicineService medicineService;

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

        given(userService.getReviewUserInfo(userId)).willReturn(userInfo);
        given(medicineService.existsById(medicineId)).willReturn(true);
        given(medicineReviewService.create(userInfo, medicineId, request)).willReturn(1L);

        //when
        Long response = medicineReviewFacade.createReview(userId, medicineId, request);

        //then
        assertThat(response).isEqualTo(1L);
        verify(userService, times(1)).getReviewUserInfo(userId);
        verify(medicineService, times(1)).existsById(medicineId);
        verify(medicineReviewService, times(1)).create(userInfo, medicineId, request);
    }

    @DisplayName("약 리뷰를 생성할 유저가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserDoesNotExistDuringReviewCreation() {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 1L;

        CreateMedicineReviewApplicationRequest request = CreateMedicineReviewApplicationRequest.builder()
                .symptom("symptom")
                .review("약이 정말 좋네요.")
                .rating(5.0)
                .build();

        given(userService.getReviewUserInfo(userId)).willThrow(UserNotFoundException.class);

        //when, then
        assertThatThrownBy(() -> medicineReviewFacade.createReview(userId, medicineId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userService, times(1)).getReviewUserInfo(userId);
        verify(medicineService, never()).existsById(medicineId);
        verify(medicineReviewService, never()).create(any(UserInfo.class),eq(medicineId), eq(request));
    }

    @DisplayName("리뷰를 작성할 약이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenMedicineDoesNotExistDuringReviewCreation() {
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

        given(userService.getReviewUserInfo(userId)).willReturn(userInfo);
        given(medicineService.existsById(medicineId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> medicineReviewFacade.createReview(userId, medicineId, request))
                .isInstanceOf(MedicineNotFoundException.class)
                .hasMessage("Medicine Not Found");

        verify(userService, times(1)).getReviewUserInfo(userId);
        verify(medicineService, times(1)).existsById(medicineId);
        verify(medicineReviewService, never()).create(any(UserInfo.class), eq(medicineId), eq(request));
    }

}
