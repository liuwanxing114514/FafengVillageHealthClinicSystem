package com.fafeng.clinic.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.dto.CreateAiDraftRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftStatusRequest;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.mapper.AiDraftMapper;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.ai.vo.AiStatusVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AiDraftService {

    private static final Set<String> DRAFT_TYPES = Set.of(
            AiDraft.TYPE_INBOUND,
            AiDraft.TYPE_VISIT,
            AiDraft.TYPE_OUTBOUND,
            AiDraft.TYPE_QUERY
    );
    private static final Set<String> STATUSES = Set.of(
            AiDraft.STATUS_PENDING,
            AiDraft.STATUS_APPROVED,
            AiDraft.STATUS_REJECTED
    );

    private final AiDraftMapper draftMapper;
    private final ClinicAiProperties properties;
    private final AiProvider activeAiProvider;

    public AiDraftService(AiDraftMapper draftMapper,
                          ClinicAiProperties properties,
                          AiProvider activeAiProvider) {
        this.draftMapper = draftMapper;
        this.properties = properties;
        this.activeAiProvider = activeAiProvider;
    }

    public AiStatusVO getStatus() {
        return new AiStatusVO(
                properties.isEnabled(),
                activeAiProvider.name(),
                activeAiProvider.isAvailable()
        );
    }

    public List<AiDraftVO> list(String draftType, String status) {
        LambdaQueryWrapper<AiDraft> wrapper = new LambdaQueryWrapper<AiDraft>()
                .orderByDesc(AiDraft::getCreatedAt);
        if (draftType != null && !draftType.isBlank()) {
            wrapper.eq(AiDraft::getDraftType, draftType.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiDraft::getStatus, status.trim());
        }
        return draftMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public AiDraftVO get(Long id) {
        return toVO(requireDraft(id));
    }

    @Transactional
    public AiDraftVO create(CreateAiDraftRequest request) {
        String draftType = request.draftType().trim();
        validateDraftType(draftType);

        AiDraft draft = new AiDraft();
        draft.setDraftType(draftType);
        draft.setStatus(AiDraft.STATUS_PENDING);
        draft.setPayload(normalizePayload(request.payload()));
        draft.setSource(resolveSource(request.source()));
        draft.setCreatedAt(OffsetDateTime.now());
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.insert(draft);
        return toVO(draft);
    }

    @Transactional
    public AiDraftVO updateStatus(Long id, UpdateAiDraftStatusRequest request) {
        AiDraft draft = requireDraft(id);
        String status = request.status().trim();
        validateStatus(status);
        if (!AiDraft.STATUS_PENDING.equals(draft.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待确认草稿可变更状态");
        }
        draft.setStatus(status);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return toVO(draft);
    }

    private AiDraft requireDraft(Long id) {
        AiDraft draft = draftMapper.selectById(id);
        if (draft == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 草稿不存在");
        }
        return draft;
    }

    private void validateDraftType(String draftType) {
        if (!DRAFT_TYPES.contains(draftType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿类型无效");
        }
    }

    private void validateStatus(String status) {
        if (!STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿状态无效");
        }
    }

    private String normalizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return "{}";
        }
        return payload.trim();
    }

    private String resolveSource(String source) {
        if (source != null && !source.isBlank()) {
            return source.trim();
        }
        return activeAiProvider.name();
    }

    private AiDraftVO toVO(AiDraft draft) {
        return new AiDraftVO(
                draft.getId(),
                draft.getDraftType(),
                draft.getStatus(),
                draft.getPayload(),
                draft.getSource(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
