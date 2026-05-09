package com.safetwin.risk.service;

import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.Risk;
import com.safetwin.repository.RiskRepository;
import com.safetwin.risk.dto.RiskDetailResponse;
import com.safetwin.risk.dto.RiskStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskService {

    private final RiskRepository riskRepository;

    public List<RiskDetailResponse> list(Long managerId, Long siteId, Risk.Status status) {
        return riskRepository.findByManagerIdWithFilters(managerId, siteId, status).stream()
                .map(RiskDetailResponse::from)
                .toList();
    }

    @Transactional
    public RiskDetailResponse updateStatus(Long riskId, RiskStatusUpdateRequest request, Long managerId) {
        Risk risk = riskRepository.findByIdAndManagerId(riskId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.RISK_NOT_FOUND));

        risk.updateStatus(request.getStatus());
        return RiskDetailResponse.from(risk);
    }
}
