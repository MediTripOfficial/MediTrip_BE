package com.meditrip.medicine.application;

import com.meditrip.medicine.application.dto.MedicineInfo;
import com.meditrip.medicine.application.dto.TakenMedicineInfo;
import com.meditrip.medicine.application.dto.response.MedicineHistoryResponse;
import com.meditrip.medicine.domain.entity.Hashtag.HashtagType;
import com.meditrip.medicine.domain.entity.MedicineReview;
import com.meditrip.medicine.domain.exception.MedicineNotFoundException;
import com.meditrip.medicine.domain.repository.MedicineHistoryQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineQueryRepository;
import com.meditrip.medicine.domain.repository.MedicineRepository;
import com.meditrip.medicine.domain.repository.MedicineReviewRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineService {

    private final MedicineQueryRepository medicineQueryRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineReviewRepository medicineReviewRepository;
    private final MedicineHistoryQueryRepository medicineHistoryQueryRepository;

    @Transactional(readOnly = true)
    public MedicineInfo getInfo(Long medicineId, UUID userId) {
        Optional<MedicineInfo> medicineInfo = medicineQueryRepository.findInfoById(medicineId);
        if (medicineInfo.isEmpty()) {
            log.info("존재하지 않는 약 정보 조회 요청. User Id : [{}], medicineId : [{}]", userId, medicineId);
            throw new MedicineNotFoundException();
        }

        return medicineInfo.get();
    }

    @Transactional(readOnly = true)
    public MedicineReview getMedicineTopReview(Long medicineId) {
        Optional<MedicineReview> topReview =
                medicineReviewRepository.findTopByMedicineIdAndIsDeletedFalseOrderByIdDesc(medicineId);

        return topReview.orElse(null);
    }

    public boolean existsById(Long medicineId) {
        return medicineRepository.existsById(medicineId);
    }

    @Transactional(readOnly = true)
    public Map<Long, MedicineInfo> getMedicineInfoByIds(List<Long> medicineIds) {
        return medicineQueryRepository.findAllByIds(medicineIds);
    }

    @Transactional(readOnly = true)
    public Page<MedicineHistoryResponse> findByMedicineHistoriesByUserId(UUID userId, LocalDate startDate,
                                                                         LocalDate endDate, PageRequest pageRequest) {
        Page<TakenMedicineInfo> histories =
                medicineHistoryQueryRepository.findHistories(userId, startDate, endDate, pageRequest);

        if (histories.isEmpty()) {
            return new PageImpl<>(List.of(), pageRequest, histories.getTotalElements());
        }

        List<Long> medicineId = histories.getContent().stream()
                .map(TakenMedicineInfo::getMedicineId)
                .distinct()
                .toList();

        Map<Long, List<String>> hashtags =
                medicineQueryRepository.fetchHashtagsGrouped(medicineId, HashtagType.DISEASE);

        return histories.map(h -> MedicineHistoryResponse.builder()
                .medicineId(h.getMedicineId())
                .medicineName(h.getName())
                .image(h.getImage())
                .date(h.getDate().atZone(ZoneId.of("Asia/Seoul")).toLocalDate())
                .symptoms(hashtags.getOrDefault(h.getMedicineId(), List.of()))
                .build());
    }

}
