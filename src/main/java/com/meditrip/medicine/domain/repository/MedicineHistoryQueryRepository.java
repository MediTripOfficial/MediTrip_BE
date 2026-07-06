package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.application.dto.TakenMedicineInfo;
import com.meditrip.medicine.domain.entity.QMedicine;
import com.meditrip.medicine.domain.entity.QMedicineIntake;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MedicineHistoryQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<TakenMedicineInfo> findHistories(UUID userId, LocalDate startDate, LocalDate endDate,
                                                 Pageable pageable) {
        QMedicineIntake intake = QMedicineIntake.medicineIntake;
        QMedicine medicine = QMedicine.medicine;

        BooleanBuilder condition = new BooleanBuilder()
                .and(intake.userId.eq(userId))
                .and(intake.isDeleted.isFalse());

        if (startDate != null) {
            condition.and(intake.firstTakenAt.goe(startDate.atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (endDate != null) {
            condition.and(intake.firstTakenAt.lt(endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }

        List<TakenMedicineInfo> content = jpaQueryFactory
                .select(Projections.constructor(TakenMedicineInfo.class,
                        medicine.id, medicine.nameEn, intake.firstTakenAt, medicine.imageUrl))
                .from(intake)
                .join(medicine).on(medicine.id.eq(intake.medicineId))
                .where(condition)
                .orderBy(intake.firstTakenAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(intake.count())
                .from(intake)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

}
