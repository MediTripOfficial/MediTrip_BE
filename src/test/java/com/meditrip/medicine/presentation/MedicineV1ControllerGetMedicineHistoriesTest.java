package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.meditrip.medicine.application.dto.response.MedicineHistoryResponse;
import com.meditrip.medicine.domain.entity.Hashtag;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineHashtags;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.repository.HashtagRepository;
import com.meditrip.medicine.domain.repository.MedicineHashtagsRepository;
import com.meditrip.medicine.domain.repository.MedicineIntakeRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class MedicineV1ControllerGetMedicineHistoriesTest extends ControllerTestSupport {

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private MedicineHashtagsRepository medicineHashtagsRepository;

    @Autowired
    private MedicineIntakeRepository medicineIntakeRepository;

    @AfterEach
    void tearDown() {
        medicineRepository.deleteAllInBatch();
        hashtagRepository.deleteAllInBatch();
        medicineHashtagsRepository.deleteAllInBatch();
        medicineIntakeRepository.deleteAllInBatch();
    }

    @DisplayName("본인이 복용한 약 정보 조회에 성공한다.")
    @Test
    void shouldRetrieveTakenMedicineInformationSuccessfully() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine1 = medicineRepository.save(Medicine.builder()
                .nameEn("Tylenol")
                .imageUrl("https://www.s3.com/tylenol")
                .build());

        Medicine medicine2 = medicineRepository.save(Medicine.builder()
                .nameEn("Children’s Tylenol Suspension")
                .imageUrl("https://www.s3.com/tylenol2")
                .build());

        Medicine medicine3 = medicineRepository.save(Medicine.builder()
                .nameEn("Mentholatum Lotion")
                .imageUrl("https://www.s3.com/mentholatum-lotion")
                .build());

        MedicineIntake medicineIntake1 = MedicineIntake.builder()
                .medicineId(medicine1.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 6, 12).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake2 = MedicineIntake.builder()
                .medicineId(medicine2.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake3 = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake anotherUserMedicineIntake = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        medicineIntakeRepository.saveAll(List.of(medicineIntake1, medicineIntake2, medicineIntake3));

        Hashtag savedHashtag1 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#fever").build());
        Hashtag savedHashtag2 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#headache").build());

        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine1.getId()).hashtagId(savedHashtag1.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine2.getId()).hashtagId(savedHashtag2.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine3.getId()).hashtagId(savedHashtag2.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/histories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<MedicineHistoryResponse> content = objectMapper.convertValue(
                resultNode.get("content"),
                new TypeReference<List<MedicineHistoryResponse>>() {
                });

        assertThat(content).hasSize(3)
                .extracting("medicineId", "medicineName", "date")
                .containsExactly(
                        tuple(medicine3.getId(), "Mentholatum Lotion", LocalDate.of(2026, 7, 5)),
                        tuple(medicine2.getId(), "Children’s Tylenol Suspension", LocalDate.of(2026, 7, 2)),
                        tuple(medicine1.getId(), "Tylenol", LocalDate.of(2026, 6, 12))
                );
    }

    @DisplayName("size를 지정해서 요청하면 해당 사이즈만큼 반환한다.")
    @Test
    void shouldReturnSpecifiedNumberOfRecords_whenSizeIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine1 = medicineRepository.save(Medicine.builder()
                .nameEn("Tylenol")
                .imageUrl("https://www.s3.com/tylenol")
                .build());

        Medicine medicine2 = medicineRepository.save(Medicine.builder()
                .nameEn("Children’s Tylenol Suspension")
                .imageUrl("https://www.s3.com/tylenol2")
                .build());

        Medicine medicine3 = medicineRepository.save(Medicine.builder()
                .nameEn("Mentholatum Lotion")
                .imageUrl("https://www.s3.com/mentholatum-lotion")
                .build());

        MedicineIntake medicineIntake1 = MedicineIntake.builder()
                .medicineId(medicine1.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 6, 12).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake2 = MedicineIntake.builder()
                .medicineId(medicine2.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake3 = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        medicineIntakeRepository.saveAll(List.of(medicineIntake1, medicineIntake2, medicineIntake3));

        Hashtag savedHashtag1 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#fever").build());
        Hashtag savedHashtag2 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#headache").build());

        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine1.getId()).hashtagId(savedHashtag1.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine2.getId()).hashtagId(savedHashtag2.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine3.getId()).hashtagId(savedHashtag2.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/histories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<MedicineHistoryResponse> content = objectMapper.convertValue(
                resultNode.get("content"),
                new TypeReference<List<MedicineHistoryResponse>>() {
                });

        assertThat(content).hasSize(2)
                .extracting("medicineId", "medicineName", "date")
                .containsExactly(
                        tuple(medicine3.getId(), "Mentholatum Lotion", LocalDate.of(2026, 7, 5)),
                        tuple(medicine2.getId(), "Children’s Tylenol Suspension", LocalDate.of(2026, 7, 2))
                );
    }

    @DisplayName("page를 지정해서 요청하면 해당 page를 반환한다.")
    @Test
    void shouldReturnSpecificPage_whenPageNumberIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine1 = medicineRepository.save(Medicine.builder()
                .nameEn("Tylenol")
                .imageUrl("https://www.s3.com/tylenol")
                .build());

        Medicine medicine2 = medicineRepository.save(Medicine.builder()
                .nameEn("Children’s Tylenol Suspension")
                .imageUrl("https://www.s3.com/tylenol2")
                .build());

        Medicine medicine3 = medicineRepository.save(Medicine.builder()
                .nameEn("Mentholatum Lotion")
                .imageUrl("https://www.s3.com/mentholatum-lotion")
                .build());

        MedicineIntake medicineIntake1 = MedicineIntake.builder()
                .medicineId(medicine1.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 6, 12).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake2 = MedicineIntake.builder()
                .medicineId(medicine2.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake3 = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        medicineIntakeRepository.saveAll(List.of(medicineIntake1, medicineIntake2, medicineIntake3));

        Hashtag savedHashtag1 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#fever").build());
        Hashtag savedHashtag2 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#headache").build());

        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine1.getId()).hashtagId(savedHashtag1.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine2.getId()).hashtagId(savedHashtag2.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine3.getId()).hashtagId(savedHashtag2.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/histories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("size", "2")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<MedicineHistoryResponse> content = objectMapper.convertValue(
                resultNode.get("content"),
                new TypeReference<List<MedicineHistoryResponse>>() {
                });

        assertThat(content).hasSize(1)
                .extracting("medicineId", "medicineName", "date")
                .containsExactly(
                        tuple(medicine1.getId(), "Tylenol", LocalDate.of(2026, 6, 12))
                );
    }

    @DisplayName("startDate를 지정하면 해당 날짜부터 복용한 약만 반환한다.")
    @Test
    void shouldReturnMedicinesTakenSinceStartDate_whenStartDateIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine1 = medicineRepository.save(Medicine.builder()
                .nameEn("Tylenol")
                .imageUrl("https://www.s3.com/tylenol")
                .build());

        Medicine medicine2 = medicineRepository.save(Medicine.builder()
                .nameEn("Children’s Tylenol Suspension")
                .imageUrl("https://www.s3.com/tylenol2")
                .build());

        Medicine medicine3 = medicineRepository.save(Medicine.builder()
                .nameEn("Mentholatum Lotion")
                .imageUrl("https://www.s3.com/mentholatum-lotion")
                .build());

        MedicineIntake medicineIntake1 = MedicineIntake.builder()
                .medicineId(medicine1.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 6, 12).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake2 = MedicineIntake.builder()
                .medicineId(medicine2.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake3 = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake anotherUserMedicineIntake = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        medicineIntakeRepository.saveAll(List.of(medicineIntake1, medicineIntake2, medicineIntake3));

        Hashtag savedHashtag1 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#fever").build());
        Hashtag savedHashtag2 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#headache").build());

        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine1.getId()).hashtagId(savedHashtag1.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine2.getId()).hashtagId(savedHashtag2.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine3.getId()).hashtagId(savedHashtag2.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/histories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("start-date", "2026-07-01"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<MedicineHistoryResponse> content = objectMapper.convertValue(
                resultNode.get("content"),
                new TypeReference<List<MedicineHistoryResponse>>() {
                });

        assertThat(content).hasSize(2)
                .extracting("medicineId", "medicineName", "date")
                .containsExactly(
                        tuple(medicine3.getId(), "Mentholatum Lotion", LocalDate.of(2026, 7, 5)),
                        tuple(medicine2.getId(), "Children’s Tylenol Suspension", LocalDate.of(2026, 7, 2))
                );
    }

    @DisplayName("end Date를 지정하면 해당 날짜까지 복용한 약만 반환한다.")
    @Test
    void shouldReturnMedicinesTakenUntilEndDate_whenEndDateIsProvided() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine1 = medicineRepository.save(Medicine.builder()
                .nameEn("Tylenol")
                .imageUrl("https://www.s3.com/tylenol")
                .build());

        Medicine medicine2 = medicineRepository.save(Medicine.builder()
                .nameEn("Children’s Tylenol Suspension")
                .imageUrl("https://www.s3.com/tylenol2")
                .build());

        Medicine medicine3 = medicineRepository.save(Medicine.builder()
                .nameEn("Mentholatum Lotion")
                .imageUrl("https://www.s3.com/mentholatum-lotion")
                .build());

        MedicineIntake medicineIntake1 = MedicineIntake.builder()
                .medicineId(medicine1.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 6, 12).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake2 = MedicineIntake.builder()
                .medicineId(medicine2.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 2).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake medicineIntake3 = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(userId)
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 3).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        MedicineIntake anotherUserMedicineIntake = MedicineIntake.builder()
                .medicineId(medicine3.getId())
                .userId(UUID.randomUUID())
                .isDeleted(false)
                .firstTakenAt(LocalDate.of(2026, 7, 5).atStartOfDay().toInstant(ZoneOffset.UTC))
                .build();

        medicineIntakeRepository.saveAll(List.of(medicineIntake1, medicineIntake2, medicineIntake3));

        Hashtag savedHashtag1 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#fever").build());
        Hashtag savedHashtag2 = hashtagRepository.save(
                Hashtag.builder().type(HashtagType.DISEASE).name("#headache").build());

        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine1.getId()).hashtagId(savedHashtag1.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine2.getId()).hashtagId(savedHashtag2.getId()).build());
        medicineHashtagsRepository.save(
                MedicineHashtags.builder().medicineId(medicine3.getId()).hashtagId(savedHashtag2.getId()).build());

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/medicines/histories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("end-date", "2026-07-02"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        JsonNode resultNode = objectMapper.readTree(
                mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<MedicineHistoryResponse> content = objectMapper.convertValue(
                resultNode.get("content"),
                new TypeReference<List<MedicineHistoryResponse>>() {
                });

        assertThat(content).hasSize(2)
                .extracting("medicineId", "medicineName", "date")
                .containsExactly(
                        tuple(medicine2.getId(), "Children’s Tylenol Suspension", LocalDate.of(2026, 7, 2)),
                        tuple(medicine1.getId(), "Tylenol", LocalDate.of(2026, 6, 12))
                );
    }

}
