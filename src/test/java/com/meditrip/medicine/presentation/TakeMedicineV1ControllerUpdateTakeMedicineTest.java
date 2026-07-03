package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.medicine.domain.TakeMedicineCondition;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.MedicineIntakeLogRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import com.meditrip.medicine.presentation.dto.request.UpdateTakeMedicineRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class TakeMedicineV1ControllerUpdateTakeMedicineTest extends ControllerTestSupport {

    @Autowired
    private MedicineIntakeRepository medicineIntakeRepository;

    @Autowired
    private MedicineIntakeLogRepository medicineIntakeLogRepository;

    @AfterEach
    void tearDown() {
        medicineIntakeRepository.deleteAllInBatch();
        medicineIntakeLogRepository.deleteAllInBatch();
    }

    @DisplayName("복약 기록이 존재하면 복약 정보 F/U에 성공한다.")
    @Test
    void shouldFollowUpIntakeSuccessfully_whenIntakeRecordExists() throws Exception {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();

        MedicineIntake medicineIntake = MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(Instant.now().minusSeconds(10000))
                .lastTakenAt(Instant.now().minusSeconds(1000000000))
                .lastCondition(TakeMedicineCondition.W)
                .build();

        MedicineIntake savedMedicineIntake = medicineIntakeRepository.save(medicineIntake);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateTakeMedicineRequest request = UpdateTakeMedicineRequest.builder()
                .condition("G")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/medicines/me/" + savedMedicineIntake.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        //then
        MedicineIntake after = medicineIntakeRepository.findById(savedMedicineIntake.getId()).orElseThrow();

        assertThat(after.getLastTakenAt()).isNotNull();
        assertThat(after.getLastTakenAt()).isNotEqualTo(medicineIntake.getLastTakenAt());
        assertThat(after.getLastCondition()).isEqualTo(TakeMedicineCondition.G);

        assertThat(medicineIntakeLogRepository.count()).isOne();
    }

    @DisplayName("복약 기록이 존재하지 않으면 복약 정보 F/U에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenIntakeRecordDoesNotExistMedicineFollowUp() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateTakeMedicineRequest request = UpdateTakeMedicineRequest.builder()
                .condition("G")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/medicines/me/" + 1111)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Medicine Intake Not Found"));

        assertThat(medicineIntakeLogRepository.count()).isZero();
    }

    @DisplayName("본인 소유가 아닌 복약 정보 F/U을 요청하면 복약 정보 F/U에 실패하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenRequestingFollowUpOnOthersIntakeRecord() throws Exception {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();

        MedicineIntake medicineIntake = MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(anotherUserId)
                .isDeleted(false)
                .firstTakenAt(Instant.now().minusSeconds(10000))
                .lastTakenAt(Instant.now().minusSeconds(100))
                .lastCondition(TakeMedicineCondition.W)
                .build();

        MedicineIntake savedMedicineIntake = medicineIntakeRepository.save(medicineIntake);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateTakeMedicineRequest request = UpdateTakeMedicineRequest.builder()
                .condition("G")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/medicines/me/" + savedMedicineIntake.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this medication record."));

        MedicineIntake after = medicineIntakeRepository.findById(savedMedicineIntake.getId()).orElseThrow();

        assertThat(after.getLastTakenAt()).isNotNull();
        assertThat(after.getLastTakenAt()).isEqualTo(medicineIntake.getLastTakenAt());

        assertThat(after.getLastCondition()).isNotEqualTo(TakeMedicineCondition.G);

        assertThat(medicineIntakeLogRepository.count()).isZero();
    }

    @DisplayName("condition이 null이면 복약 정보 F/U에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenConditionIsNullDuringFollowUp() throws Exception {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();

        MedicineIntake medicineIntake = MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(Instant.now().minusSeconds(10000))
                .lastTakenAt(Instant.now().minusSeconds(100))
                .lastCondition(TakeMedicineCondition.W)
                .build();

        MedicineIntake savedMedicineIntake = medicineIntakeRepository.save(medicineIntake);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateTakeMedicineRequest request = UpdateTakeMedicineRequest.builder()
                .build();

        //when
        mockMvc.perform(patch("/api/v1/medicines/me/" + savedMedicineIntake.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Condition is required."));

        MedicineIntake after = medicineIntakeRepository.findById(savedMedicineIntake.getId()).orElseThrow();

        assertThat(after.getLastTakenAt()).isNotNull();
        assertThat(after.getLastTakenAt()).isEqualTo(medicineIntake.getLastTakenAt());

        assertThat(after.getLastCondition()).isNotEqualTo(TakeMedicineCondition.G);

        assertThat(medicineIntakeLogRepository.count()).isZero();
    }

    @DisplayName("condition이 공백이면 복약 정보 F/U에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenConditionIsBlankDuringFollowUp() throws Exception {
        //given
        Long medicineId = 1L;
        UUID userId = UUID.randomUUID();

        MedicineIntake medicineIntake = MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(Instant.now().minusSeconds(10000))
                .lastTakenAt(Instant.now().minusSeconds(100))
                .lastCondition(TakeMedicineCondition.W)
                .build();

        MedicineIntake savedMedicineIntake = medicineIntakeRepository.save(medicineIntake);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        UpdateTakeMedicineRequest request = UpdateTakeMedicineRequest.builder()
                .condition(" ")
                .build();

        //when
        mockMvc.perform(patch("/api/v1/medicines/me/" + savedMedicineIntake.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Condition is required."));

        MedicineIntake after = medicineIntakeRepository.findById(savedMedicineIntake.getId()).orElseThrow();

        assertThat(after.getLastTakenAt()).isNotNull();
        assertThat(after.getLastTakenAt()).isEqualTo(medicineIntake.getLastTakenAt());

        assertThat(after.getLastCondition()).isNotEqualTo(TakeMedicineCondition.G);

        assertThat(medicineIntakeLogRepository.count()).isZero();
    }

}
