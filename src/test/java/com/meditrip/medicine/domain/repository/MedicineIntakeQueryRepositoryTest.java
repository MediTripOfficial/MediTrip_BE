package com.meditrip.medicine.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.meditrip.medicine.domain.IntakeSortType;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MedicineIntakeQueryRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    private MedicineIntakeQueryRepository medicineIntakeQueryRepository;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        medicineIntakeQueryRepository = new MedicineIntakeQueryRepository(queryFactory);
    }

    private MedicineIntake persistIntake(Long medicineId, UUID userId, boolean isDeleted, Instant firstTakenAt) {
        MedicineIntake intake = MedicineIntake.builder()
                .medicineId(medicineId)
                .userId(userId)
                .isDeleted(isDeleted)
                .firstTakenAt(firstTakenAt)
                .build();
        entityManager.persist(intake);
        return intake;
    }

    @DisplayName("본인의 복약 이력만 조회되고, 다른 유저의 이력은 포함되지 않는다.")
    @Test
    void shouldReturnOnlyOwnIntakes_whenOtherUsersIntakesExist() {
        //given
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();

        persistIntake(1L, userId, false, Instant.now());
        persistIntake(2L, anotherUserId, false, Instant.now());

        entityManager.flush();
        entityManager.clear();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, null, IntakeSortType.LATEST, PageRequest.of(0, 10));

        //then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);
    }

    @DisplayName("삭제된 복약 이력은 조회 결과에서 제외된다.")
    @Test
    void shouldExcludeDeletedIntakes() {
        //given
        UUID userId = UUID.randomUUID();

        persistIntake(1L, userId, false, Instant.now());
        persistIntake(2L, userId, true, Instant.now());

        entityManager.flush();
        entityManager.clear();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, null, IntakeSortType.LATEST, PageRequest.of(0, 10));

        //then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMedicineId()).isEqualTo(1L);
    }

    @DisplayName("firstStartDate, firstEndDate 범위 안에 있는 복약 이력만 조회된다.")
    @Test
    void shouldFilterByFirstTakenAtDateRange() {
        //given
        UUID userId = UUID.randomUUID();

        Instant beforeRange = LocalDate.of(2024, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant withinRange = LocalDate.of(2024, 5, 15).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant afterRange = LocalDate.of(2024, 12, 31).atStartOfDay(ZoneOffset.UTC).toInstant();

        persistIntake(1L, userId, false, beforeRange);
        persistIntake(2L, userId, false, withinRange);
        persistIntake(3L, userId, false, afterRange);

        entityManager.flush();
        entityManager.clear();

        LocalDate firstStartDate = LocalDate.of(2024, 3, 1);
        LocalDate firstEndDate = LocalDate.of(2024, 9, 1);

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, firstStartDate, firstEndDate, IntakeSortType.LATEST, PageRequest.of(0, 10));

        //then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMedicineId()).isEqualTo(2L);
    }

    @DisplayName("firstEndDate 당일에 복용한 이력도 조회 결과에 포함된다.")
    @Test
    void shouldIncludeIntakeOnFirstEndDate() {
        //given
        UUID userId = UUID.randomUUID();
        LocalDate firstEndDate = LocalDate.of(2024, 5, 15);
        Instant onEndDate = firstEndDate.atTime(23, 0).toInstant(ZoneOffset.UTC);

        persistIntake(1L, userId, false, onEndDate);

        entityManager.flush();
        entityManager.clear();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, firstEndDate, IntakeSortType.LATEST, PageRequest.of(0, 10));

        //then
        assertThat(result.getContent()).hasSize(1);
    }

    @DisplayName("LATEST 정렬 시 firstTakenAt 기준 내림차순으로 정렬되어 조회된다.")
    @Test
    void shouldSortByFirstTakenAtDescending_whenSortIsLatest() {
        //given
        UUID userId = UUID.randomUUID();

        Instant oldest = Instant.now().minusSeconds(3000);
        Instant middle = Instant.now().minusSeconds(2000);
        Instant newest = Instant.now().minusSeconds(1000);

        persistIntake(1L, userId, false, oldest);
        persistIntake(2L, userId, false, newest);
        persistIntake(3L, userId, false, middle);

        entityManager.flush();
        entityManager.clear();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, null, IntakeSortType.LATEST, PageRequest.of(0, 10));

        //then
        assertThat(result.getContent())
                .extracting(MedicineIntake::getMedicineId)
                .containsExactly(2L, 3L, 1L);
    }

    @DisplayName("요청한 size보다 이력이 많으면 해당 페이지 개수만큼만 반환되고, totalElements는 전체 개수와 같다.")
    @Test
    void shouldReturnPagedResult_whenIntakeCountExceedsRequestedSize() {
        //given
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            persistIntake((long) (i + 1), userId, false, Instant.now().minusSeconds(i * 1000L));
        }

        entityManager.flush();
        entityManager.clear();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, null, IntakeSortType.LATEST, PageRequest.of(0, 2));

        //then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
    }

    @DisplayName("두 번째 페이지를 요청하면 offset만큼 건너뛴 결과가 조회된다.")
    @Test
    void shouldReturnSecondPage_whenPageIndexIsOne() {
        //given
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            persistIntake((long) (i + 1), userId, false, Instant.now().minusSeconds(i * 1000L));
        }

        entityManager.flush();
        entityManager.clear();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, null, IntakeSortType.LATEST, PageRequest.of(1, 2));

        //then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(MedicineIntake::getMedicineId)
                .containsExactly(3L, 4L);
    }

    @DisplayName("조회 조건에 맞는 복약 이력이 없으면 빈 페이지를 반환한다.")
    @Test
    void shouldReturnEmptyPage_whenNoIntakesMatch() {
        //given
        UUID userId = UUID.randomUUID();

        //when
        Page<MedicineIntake> result = medicineIntakeQueryRepository.findIntakes(
                userId, null, null, IntakeSortType.LATEST, PageRequest.of(0, 10));

        //then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

}
