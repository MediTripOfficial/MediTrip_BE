package com.meditrip.medicine.domain.repository;

import com.meditrip.medicine.domain.ReviewSortType;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.entity.QMedicineReview;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MedicineReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MedicineReview> findReviews(Long medicineId, Long cursor, int limit, ReviewSortType sort,
                                             List<String> genders, List<String> countries,
                                             List<String> symptoms) {

        QMedicineReview review = QMedicineReview.medicineReview;

        BooleanBuilder where = new BooleanBuilder()
                .and(review.medicineId.eq(medicineId))
                .and(review.isDeleted.isFalse());

        if (genders != null && !genders.isEmpty()) {
            where.and(review.gender.in(genders));
        }
        if (countries != null && !countries.isEmpty()) {
            where.and(review.country.in(countries));
        }
        if (symptoms != null && !symptoms.isEmpty()) {
            where.and(review.symptom.in(symptoms));
        }
        if (cursor != null) {
            where.and(buildCursorPredicate(review, cursor, sort));
        }

        return queryFactory
                .selectFrom(review)
                .where(where)
                .orderBy(buildOrderSpecifiers(review, sort))
                .limit(limit)
                .fetch();
    }

    private BooleanBuilder buildCursorPredicate(QMedicineReview review, Long cursorId, ReviewSortType sort) {
        MedicineReview cursorReview = queryFactory
                .selectFrom(review)
                .where(review.id.eq(cursorId))
                .fetchOne();

        if (cursorReview == null) {
            return new BooleanBuilder(review.id.lt(cursorId));
        }

        return switch (sort) {
            case LATEST -> new BooleanBuilder(review.id.lt(cursorId));
            case HIGHEST_RATING -> new BooleanBuilder(
                    review.rating.lt(cursorReview.getRating())
                            .or(review.rating.eq(cursorReview.getRating()).and(review.id.lt(cursorId))));
            case LOWEST_RATING -> new BooleanBuilder(
                    review.rating.gt(cursorReview.getRating())
                            .or(review.rating.eq(cursorReview.getRating()).and(review.id.lt(cursorId))));
        };
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(QMedicineReview review, ReviewSortType sort) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier<?>[]{review.id.desc()};
            case HIGHEST_RATING -> new OrderSpecifier<?>[]{review.rating.desc(), review.id.desc()};
            case LOWEST_RATING -> new OrderSpecifier<?>[]{review.rating.asc(), review.id.desc()};
        };
    }

}
