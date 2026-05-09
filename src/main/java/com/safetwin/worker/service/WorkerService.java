package com.safetwin.worker.service;

import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.Site;
import com.safetwin.entity.Worker;
import com.safetwin.repository.SiteRepository;
import com.safetwin.repository.WorkerRepository;
import com.safetwin.worker.dto.WorkerCreateRequest;
import com.safetwin.worker.dto.WorkerResponse;
import com.safetwin.worker.dto.WorkerUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;

    @Transactional
    public WorkerResponse create(Long siteId, WorkerCreateRequest request, Long managerId) {
        Site site = siteRepository.findByIdAndManagerId(siteId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.SITE_NOT_FOUND));

        Worker worker = Worker.builder()
                .site(site)
                .name(request.getName())
                .phone(request.getPhone())
                .occupation(request.getOccupation())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return WorkerResponse.from(workerRepository.save(worker));
    }

    public List<WorkerResponse> list(Long siteId, Long managerId) {
        siteRepository.findByIdAndManagerId(siteId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.SITE_NOT_FOUND));

        return workerRepository.findBySiteIdAndSiteManagerId(siteId, managerId).stream()
                .map(WorkerResponse::from)
                .toList();
    }

    @Transactional
    public WorkerResponse update(Long workerId, WorkerUpdateRequest request, Long managerId) {
        Worker worker = workerRepository.findByIdAndSiteManagerId(workerId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.WORKER_NOT_FOUND));

        worker.update(request.getName(), request.getPhone(), request.getOccupation(),
                request.getStartDate(), request.getEndDate());
        return WorkerResponse.from(worker);
    }

    @Transactional
    public void delete(Long workerId, Long managerId) {
        Worker worker = workerRepository.findByIdAndSiteManagerId(workerId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.WORKER_NOT_FOUND));
        workerRepository.delete(worker);
    }
}
