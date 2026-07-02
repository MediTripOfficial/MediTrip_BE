package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TakeMedicineV1ControllerTakeMedicineTest extends ControllerTestSupport {

    @Autowired
    private MedicineIntakeRepository medicineIntakeRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @BeforeEach
    void setUp() {
        medicineIntakeRepository.deleteAllInBatch();
        medicineRepository.deleteAllInBatch();
    }

    @DisplayName("약 정보가 존재하면 복약 정보 삽입에 성공한다.")
    @Test
    void shouldSaveIntakeInformationSuccessfully_whenMedicineExists() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                .param("medicineId", String.valueOf(savedMedicine.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/medicines/me/1"));

        //then
        MedicineIntake after = medicineIntakeRepository.findById(1L).orElseThrow();
        assertThat(after.getMedicineId()).isEqualTo(savedMedicine.getId());
        assertThat(after.getUserId()).isEqualTo(userId);
        assertThat(after.getFirstTakenAt()).isNotNull();
        assertThat(after.isDeleted()).isFalse();
    }

    @DisplayName("약 정보가 존재하지 않으면 복약 정보 삽입에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenMedicineDoesNotExistDuringIntakeCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        Long medicineId = 11L;

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when, then
        mockMvc.perform(post("/api/v1/medicines/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                        .param("medicineId", String.valueOf(medicineId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Medicine Not Found"));

        assertThat(medicineIntakeRepository.count()).isZero();
    }

}
