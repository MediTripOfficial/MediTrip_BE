package com.meditrip.medicine.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.medicine.domain.entity.Medicine;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import com.meditrip.medicine.presentation.dto.request.CreateMedicineReviewRequest;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MedicineReviewV1controllerCreateTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineReviewRepository medicineReviewRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        medicineRepository.deleteAllInBatch();
        medicineReviewRepository.deleteAllInBatch();
    }

    @DisplayName("유저와 약 정보가 존재하면 약 리뷰 생성에 성공한다.")
    @Test
    void shouldCreateMedicineReviewSuccessfully_whenUserAndMedicineExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(5.0)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/reviews/1"));

        MedicineReview savedReview = medicineReviewRepository.findById(1L).orElseThrow();
        assertThat(savedReview.getReview()).isEqualTo(request.getReview());
        assertThat(savedReview.getRating()).isEqualTo(request.getRating());
        assertThat(savedReview.getSymptom()).isEqualTo(request.getSymptom());
        assertThat(savedReview.getMedicineId()).isEqualTo(savedMedicine.getId());
        assertThat(savedReview.getHeight()).isEqualTo(user.getHeight());
        assertThat(savedReview.getWeight()).isEqualTo(user.getWeight());
        assertThat(savedReview.getCountry()).isEqualTo(user.getCountry());
        assertThat(savedReview.getIsDeleted()).isFalse();
    }

    @DisplayName("약 리뷰를 생성 할 유저 정보가 없으면 404를 반환하고, 약 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn404_whenUserInformationIsMissingDuringReviewCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(5.0)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("약 리뷰를 생성 할 유저가 이미 탈퇴했거나 삭제된 유저, 게스트 유저라면 404를 반환하고, 약 리뷰 생성에 실패한다.")
    @ParameterizedTest(name = "[{index}] 유저 상태 : {0}")
    @EnumSource(value = UserStatus.class, names = {"DELETED", "WITHDRAWN", "GUEST"})
    void shouldReturn404_whenUserIsWithdrawnDeletedOrGuestDuringReviewCreation(UserStatus userStatus) throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, userStatus);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(5.0)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User Not Found."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("리뷰를 작성할 약 정보가 존재하지 않으면 404를 반환하고, 약 정보 생성에 실패한다.")
    @Test
    void shouldReturn404_whenMedicineDoesNotExistDuringReviewCreation() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(5.0)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + 100L + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Medicine Not Found"));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("review가 null이면 400을 반환하고, 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn400_whenReviewContentIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .rating(5.0)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Review is required."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("review가 공백이면 400을 반환하고, 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn400_whenReviewContentIsBlank() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review(" ")
                .rating(5.0)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Review is required."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("rating이 null이면 400을 반환하고, 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn400_whenRatingIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating is required."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("rating이 0.0보다 작으면 400을 반환하고, 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn400_whenRatingIsLessThanMin() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(-0.1)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating must be greater than or equal to 0.0."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("rating이 5.0보다 크면 400을 반환하고, 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn400_whenRatingIsGreaterThanMax() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(5.1)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating must be less than or equal to 5.0."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

    @DisplayName("rating의 소수점 자릿수가 둘째 자리 이상이면 400을 반환하고, 리뷰 생성에 실패한다.")
    @Test
    void shouldReturn400_whenRatingHasTooManyFractionDigits() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        User user = createUser(userId, UserStatus.ACTIVE);
        userRepository.save(user);

        Medicine medicine = Medicine.builder()
                .nameKo("타이레놀정 500mg")
                .build();

        Medicine savedMedicine = medicineRepository.save(medicine);

        CreateMedicineReviewRequest request = CreateMedicineReviewRequest.builder()
                .symptom("Symptom")
                .review("약이 정말 좋네요.")
                .rating(4.55)
                .build();

        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        //when
        mockMvc.perform(post("/api/v1/medicines/" + savedMedicine.getId() + "/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                //then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating must have up to 1 integer digit and 1 fractional digit."));

        assertThat(medicineReviewRepository.count()).isZero();
    }

}
