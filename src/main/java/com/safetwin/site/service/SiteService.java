package com.safetwin.site.service;

import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.Site;
import com.safetwin.entity.User;
import com.safetwin.repository.SiteRepository;
import com.safetwin.repository.UserRepository;
import com.safetwin.site.dto.SiteCreateRequest;
import com.safetwin.site.dto.SiteResponse;
import com.safetwin.site.dto.SiteUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteService {

    private final SiteRepository siteRepository;
    private final UserRepository userRepository;

    @Transactional
    public SiteResponse create(SiteCreateRequest request, Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        Site site = Site.builder()
                .manager(manager)
                .name(request.getName())
                .address(request.getAddress())
                .bizNumber(request.getBizNumber())
                .status(Site.Status.ACTIVE)
                .build();

        return SiteResponse.from(siteRepository.save(site));
    }

    public List<SiteResponse> list(Long managerId) {
        return siteRepository.findByManagerId(managerId).stream()
                .map(SiteResponse::from)
                .toList();
    }

    public SiteResponse getDetail(Long id, Long managerId) {
        Site site = siteRepository.findByIdAndManagerId(id, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.SITE_NOT_FOUND));
        return SiteResponse.from(site);
    }

    @Transactional
    public SiteResponse update(Long id, SiteUpdateRequest request, Long managerId) {
        Site site = siteRepository.findByIdAndManagerId(id, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.SITE_NOT_FOUND));

        site.update(request.getName(), request.getAddress(), request.getBizNumber(), request.getStatus());
        return SiteResponse.from(site);
    }

    @Transactional
    public void delete(Long id, Long managerId) {
        Site site = siteRepository.findByIdAndManagerId(id, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.SITE_NOT_FOUND));
        siteRepository.delete(site);
    }
}
