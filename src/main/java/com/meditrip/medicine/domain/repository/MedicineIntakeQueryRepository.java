package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.IntakeSortType;
import com.meditrip.medicine.domain.entity.MedicineIntake;
import com.meditrip.medicine.domain.entity.QMedicineIntake;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
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
public class MedicineIntakeQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<MedicineIntake> findIntakes(UUID userId, LocalDate firstStartDate, LocalDate firstEndDate,
                                            IntakeSortType sort, Pageable pageable) {
        QMedicineIntake intake = QMedicineIntake.medicineIntake;

        BooleanBuilder condition = new BooleanBuilder()
                .and(intake.userId.eq(userId))
                .and(intake.isDeleted.isFalse());

        if (firstStartDate != null) {
            condition.and(intake.firstTakenAt.goe(firstStartDate.atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (firstEndDate != null) {
            condition.and(intake.firstTakenAt.lt(firstEndDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }

        List<MedicineIntake> content = jpaQueryFactory
                .selectFrom(intake)
                .where(condition)
                .orderBy(resolveOrder(sort, intake))
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

    private OrderSpecifier<?> resolveOrder(IntakeSortType sort, QMedicineIntake intake) {
        return switch (sort) {
            case LATEST -> intake.firstTakenAt.desc();
        };
    }


}
