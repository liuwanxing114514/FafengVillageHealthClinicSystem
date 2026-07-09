package com.fafeng.clinic.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.ai.QuickPhraseField;
import com.fafeng.clinic.ai.dto.SaveQuickPhraseRequest;
import com.fafeng.clinic.ai.entity.QuickPhrase;
import com.fafeng.clinic.ai.mapper.QuickPhraseMapper;
import com.fafeng.clinic.ai.vo.QuickPhraseCleanupVO;
import com.fafeng.clinic.ai.vo.QuickPhraseFieldVO;
import com.fafeng.clinic.ai.vo.QuickPhraseVO;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.clinic.entity.PrescriptionItem;
import com.fafeng.clinic.clinic.mapper.ClinicVisitMapper;
import com.fafeng.clinic.clinic.mapper.PrescriptionItemMapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.system.entity.SysSetting;
import com.fafeng.clinic.system.mapper.SysSettingMapper;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class QuickPhraseService {

    private static final int DEFAULT_CANDIDATE_LIMIT = 12;
    private static final int DEFAULT_CLEANUP_DAYS = 180;
    private static final int DEFAULT_CLEANUP_MIN_COUNT = 1;

    private final QuickPhraseMapper quickPhraseMapper;
    private final ClinicVisitMapper visitMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final SysSettingMapper sysSettingMapper;
    private final AuditLogService auditLogService;

    public QuickPhraseService(QuickPhraseMapper quickPhraseMapper,
                              ClinicVisitMapper visitMapper,
                              PrescriptionItemMapper prescriptionItemMapper,
                              SysSettingMapper sysSettingMapper,
                              AuditLogService auditLogService) {
        this.quickPhraseMapper = quickPhraseMapper;
        this.visitMapper = visitMapper;
        this.prescriptionItemMapper = prescriptionItemMapper;
        this.sysSettingMapper = sysSettingMapper;
        this.auditLogService = auditLogService;
    }

    public List<QuickPhraseFieldVO> listFields() {
        return QuickPhraseField.all().stream()
                .map(field -> new QuickPhraseFieldVO(field.key(), field.label()))
                .toList();
    }

    public List<QuickPhraseVO> listCandidates(String fieldKey, Integer limit) {
        QuickPhraseField field = QuickPhraseField.require(fieldKey);
        int safeLimit = normalizeLimit(limit);
        ensureHistorySynced(field);

        return quickPhraseMapper.selectList(new LambdaQueryWrapper<QuickPhrase>()
                        .eq(QuickPhrase::getFieldKey, field.key())
                        .orderByDesc(QuickPhrase::getUseCount)
                        .orderByDesc(QuickPhrase::getLastUsedAt)
                        .orderByDesc(QuickPhrase::getUpdatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<QuickPhraseVO> listManaged(String fieldKey) {
        LambdaQueryWrapper<QuickPhrase> wrapper = new LambdaQueryWrapper<QuickPhrase>()
                .orderByAsc(QuickPhrase::getFieldKey)
                .orderByDesc(QuickPhrase::getUseCount)
                .orderByDesc(QuickPhrase::getUpdatedAt);
        if (fieldKey != null && !fieldKey.isBlank()) {
            QuickPhraseField field = QuickPhraseField.require(fieldKey);
            wrapper.eq(QuickPhrase::getFieldKey, field.key());
        }
        return quickPhraseMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public QuickPhraseVO create(SaveQuickPhraseRequest request) {
        QuickPhraseField field = QuickPhraseField.require(request.fieldKey());
        String content = normalizeContent(request.content());
        QuickPhrase existing = findByFieldAndContent(field.key(), content);
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该字段下已存在相同快捷语");
        }

        OffsetDateTime now = OffsetDateTime.now();
        QuickPhrase phrase = new QuickPhrase();
        phrase.setFieldKey(field.key());
        phrase.setContent(content);
        phrase.setSource(QuickPhrase.SOURCE_MANUAL);
        phrase.setUseCount(0);
        phrase.setCreatedAt(now);
        phrase.setUpdatedAt(now);
        quickPhraseMapper.insert(phrase);

        auditLogService.log("CREATE_QUICK_PHRASE", "quick_phrase", phrase.getId(),
                "{\"fieldKey\":\"" + field.key() + "\"}");
        return toVO(phrase);
    }

    @Transactional
    public QuickPhraseVO update(Long id, SaveQuickPhraseRequest request) {
        QuickPhrase phrase = requirePhrase(id);
        QuickPhraseField field = QuickPhraseField.require(request.fieldKey());
        String content = normalizeContent(request.content());

        QuickPhrase duplicate = findByFieldAndContent(field.key(), content);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该字段下已存在相同快捷语");
        }

        phrase.setFieldKey(field.key());
        phrase.setContent(content);
        phrase.setSource(QuickPhrase.SOURCE_MANUAL);
        phrase.setUpdatedAt(OffsetDateTime.now());
        quickPhraseMapper.updateById(phrase);

        auditLogService.log("UPDATE_QUICK_PHRASE", "quick_phrase", phrase.getId(),
                "{\"fieldKey\":\"" + field.key() + "\"}");
        return toVO(phrase);
    }

    @Transactional
    public void delete(Long id) {
        QuickPhrase phrase = requirePhrase(id);
        quickPhraseMapper.deleteById(id);
        auditLogService.log("DELETE_QUICK_PHRASE", "quick_phrase", id,
                "{\"fieldKey\":\"" + phrase.getFieldKey() + "\"}");
    }

    @Transactional
    public QuickPhraseVO recordUsage(Long id) {
        QuickPhrase phrase = requirePhrase(id);
        OffsetDateTime now = OffsetDateTime.now();
        phrase.setUseCount(safeCount(phrase.getUseCount()) + 1);
        phrase.setLastUsedAt(now);
        phrase.setUpdatedAt(now);
        quickPhraseMapper.updateById(phrase);
        return toVO(phrase);
    }

    @Transactional
    public void recordFromVisit(ClinicVisit visit) {
        if (visit == null) {
            return;
        }
        recordField(QuickPhraseField.CHIEF_COMPLAINT, visit.getChiefComplaint());
        recordField(QuickPhraseField.PRESENT_ILLNESS, visit.getPresentIllness());
        recordField(QuickPhraseField.PAST_HISTORY, visit.getPastHistory());
        recordField(QuickPhraseField.ALLERGY_HISTORY, visit.getAllergyHistory());
        recordField(QuickPhraseField.DIAGNOSIS, visit.getDiagnosis());
        recordField(QuickPhraseField.TREATMENT, visit.getTreatment());
        recordField(QuickPhraseField.REMARK, visit.getRemark());
    }

    @Transactional
    public void recordPrescriptionUsage(String usage) {
        recordField(QuickPhraseField.PRESCRIPTION_USAGE, usage);
    }

    @Transactional
    public QuickPhraseCleanupVO cleanupStale() {
        int days = readIntSetting("quick_phrase_cleanup_days", DEFAULT_CLEANUP_DAYS);
        int maxCount = readIntSetting("quick_phrase_cleanup_min_count", DEFAULT_CLEANUP_MIN_COUNT);
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(Math.max(days, 1));
        int removed = quickPhraseMapper.deleteStaleHistory(cutoff, Math.max(maxCount, 0));
        if (removed > 0) {
            auditLogService.log("CLEANUP_QUICK_PHRASE", "quick_phrase", null,
                    "{\"removed\":" + removed + "}");
        }
        return new QuickPhraseCleanupVO(removed);
    }

    @Transactional
    public void syncAllHistory() {
        for (QuickPhraseField field : QuickPhraseField.all()) {
            if (field == QuickPhraseField.PRESCRIPTION_USAGE) {
                syncPrescriptionUsageHistory();
            } else {
                syncVisitFieldHistory(field);
            }
        }
    }

    private void ensureHistorySynced(QuickPhraseField field) {
        Long count = quickPhraseMapper.selectCount(new LambdaQueryWrapper<QuickPhrase>()
                .eq(QuickPhrase::getFieldKey, field.key()));
        if (count != null && count > 0) {
            return;
        }
        if (field == QuickPhraseField.PRESCRIPTION_USAGE) {
            syncPrescriptionUsageHistory();
        } else {
            syncVisitFieldHistory(field);
        }
    }

    private void syncVisitFieldHistory(QuickPhraseField field) {
        Function<ClinicVisit, String> extractor = visitExtractor(field);
        if (extractor == null) {
            return;
        }
        List<ClinicVisit> visits = visitMapper.selectList(new LambdaQueryWrapper<ClinicVisit>()
                .eq(ClinicVisit::getStatus, ClinicVisit.STATUS_ACTIVE));
        Map<String, Integer> counts = new HashMap<>();
        for (ClinicVisit visit : visits) {
            String content = normalizeContent(extractor.apply(visit));
            if (content == null) {
                continue;
            }
            counts.merge(content, 1, Integer::sum);
        }
        upsertHistoryCounts(field.key(), counts);
    }

    private void syncPrescriptionUsageHistory() {
        List<PrescriptionItem> items = prescriptionItemMapper.selectList(new LambdaQueryWrapper<PrescriptionItem>()
                .select(PrescriptionItem::getUsage));
        Map<String, Integer> counts = new HashMap<>();
        for (PrescriptionItem item : items) {
            String content = normalizeContent(item.getUsage());
            if (content == null) {
                continue;
            }
            counts.merge(content, 1, Integer::sum);
        }
        upsertHistoryCounts(QuickPhraseField.PRESCRIPTION_USAGE.key(), counts);
    }

    private void upsertHistoryCounts(String fieldKey, Map<String, Integer> counts) {
        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            QuickPhrase existing = findByFieldAndContent(fieldKey, entry.getKey());
            if (existing == null) {
                QuickPhrase phrase = new QuickPhrase();
                phrase.setFieldKey(fieldKey);
                phrase.setContent(entry.getKey());
                phrase.setSource(QuickPhrase.SOURCE_HISTORY);
                phrase.setUseCount(entry.getValue());
                phrase.setLastUsedAt(now);
                phrase.setCreatedAt(now);
                phrase.setUpdatedAt(now);
                quickPhraseMapper.insert(phrase);
                continue;
            }
            if (QuickPhrase.SOURCE_MANUAL.equals(existing.getSource())) {
                continue;
            }
            existing.setUseCount(Math.max(safeCount(existing.getUseCount()), entry.getValue()));
            existing.setUpdatedAt(now);
            quickPhraseMapper.updateById(existing);
        }
    }

    private void recordField(QuickPhraseField field, String rawContent) {
        String content = normalizeContent(rawContent);
        if (content == null) {
            return;
        }
        QuickPhrase existing = findByFieldAndContent(field.key(), content);
        OffsetDateTime now = OffsetDateTime.now();
        if (existing == null) {
            QuickPhrase phrase = new QuickPhrase();
            phrase.setFieldKey(field.key());
            phrase.setContent(content);
            phrase.setSource(QuickPhrase.SOURCE_HISTORY);
            phrase.setUseCount(1);
            phrase.setLastUsedAt(now);
            phrase.setCreatedAt(now);
            phrase.setUpdatedAt(now);
            quickPhraseMapper.insert(phrase);
            return;
        }
        existing.setUseCount(safeCount(existing.getUseCount()) + 1);
        existing.setLastUsedAt(now);
        existing.setUpdatedAt(now);
        quickPhraseMapper.updateById(existing);
    }

    private QuickPhrase findByFieldAndContent(String fieldKey, String content) {
        return quickPhraseMapper.selectOne(new LambdaQueryWrapper<QuickPhrase>()
                .eq(QuickPhrase::getFieldKey, fieldKey)
                .eq(QuickPhrase::getContent, content));
    }

    private QuickPhrase requirePhrase(Long id) {
        QuickPhrase phrase = quickPhraseMapper.selectById(id);
        if (phrase == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "快捷语不存在");
        }
        return phrase;
    }

    private QuickPhraseVO toVO(QuickPhrase phrase) {
        String label = QuickPhraseField.fromKey(phrase.getFieldKey())
                .map(QuickPhraseField::label)
                .orElse(phrase.getFieldKey());
        return new QuickPhraseVO(
                phrase.getId(),
                phrase.getFieldKey(),
                label,
                phrase.getContent(),
                phrase.getSource(),
                safeCount(phrase.getUseCount()),
                phrase.getLastUsedAt(),
                phrase.getCreatedAt(),
                phrase.getUpdatedAt());
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_CANDIDATE_LIMIT;
        }
        return Math.min(Math.max(limit, 1), 30);
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int readIntSetting(String key, int defaultValue) {
        SysSetting setting = sysSettingMapper.selectOne(new LambdaQueryWrapper<SysSetting>()
                .eq(SysSetting::getSettingKey, key));
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(setting.getSettingValue().trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Function<ClinicVisit, String> visitExtractor(QuickPhraseField field) {
        return switch (field) {
            case CHIEF_COMPLAINT -> ClinicVisit::getChiefComplaint;
            case PRESENT_ILLNESS -> ClinicVisit::getPresentIllness;
            case PAST_HISTORY -> ClinicVisit::getPastHistory;
            case ALLERGY_HISTORY -> ClinicVisit::getAllergyHistory;
            case DIAGNOSIS -> ClinicVisit::getDiagnosis;
            case TREATMENT -> ClinicVisit::getTreatment;
            case REMARK -> ClinicVisit::getRemark;
            case PRESCRIPTION_USAGE -> null;
        };
    }

}
