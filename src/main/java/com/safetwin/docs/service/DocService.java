package com.safetwin.docs.service;

import com.safetwin.analysis.service.S3Service;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.docs.dto.*;
import com.safetwin.entity.*;
import com.safetwin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocService {

    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final AnalysisRepository analysisRepository;
    private final RiskRepository riskRepository;
    private final SiteRepository siteRepository;
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final PdfGenerationService pdfGenerationService;
    private final S3Service s3Service;

    // ── 위험성 평가서 생성 ────────────────────────────────────────────────────

    @Transactional
    public DocResponse createRiskAssessment(RiskAssessmentRequest request, Long userId) {
        Analysis analysis = analysisRepository.findByIdAndManagerId(request.getAnalysisId(), userId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        if (analysis.getStatus() != Analysis.Status.COMPLETED) {
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }

        List<Risk> risks = riskRepository.findByAnalysisId(analysis.getId());
        byte[] pdfBytes = pdfGenerationService.generateRiskAssessment(analysis, risks);

        Site site = analysis.getZone().getSite();
        String key = "docs/%d/risk-assessment-%d-%s.pdf"
                .formatted(site.getId(), analysis.getId(), UUID.randomUUID());
        String fileUrl = s3Service.uploadBytes(pdfBytes, key, "application/pdf");

        Document doc = Document.builder()
                .site(site)
                .uploader(userRepository.getReferenceById(userId))
                .analysis(analysis)
                .title("위험성 평가서 - " + analysis.getZone().getName())
                .type(Document.DocumentType.RISK_ASSESSMENT)
                .fileUrl(fileUrl)
                .fileSize((long) pdfBytes.length)
                .build();

        return DocResponse.from(documentRepository.save(doc));
    }

    // ── 교육 확인서 생성 ──────────────────────────────────────────────────────

    @Transactional
    public DocResponse createEducationCert(EducationCertRequest request, Long userId) {
        Site site = siteRepository.findByIdAndManagerId(request.getSiteId(), userId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        List<Worker> workers = request.getWorkerIds() != null && !request.getWorkerIds().isEmpty()
                ? workerRepository.findAllById(request.getWorkerIds())
                : workerRepository.findBySiteId(site.getId());

        byte[] pdfBytes = pdfGenerationService.generateEducationCert(
                site, request.getEducationDate(), request.getContent(), workers);

        String key = "docs/%d/education-cert-%s.pdf".formatted(site.getId(), UUID.randomUUID());
        String fileUrl = s3Service.uploadBytes(pdfBytes, key, "application/pdf");

        Document doc = Document.builder()
                .site(site)
                .uploader(userRepository.getReferenceById(userId))
                .title("안전보건교육 확인서 - " + request.getEducationDate())
                .type(Document.DocumentType.EDUCATION_CERT)
                .fileUrl(fileUrl)
                .fileSize((long) pdfBytes.length)
                .build();

        return DocResponse.from(documentRepository.save(doc));
    }

    // ── 단체 사진 업로드 ──────────────────────────────────────────────────────

    @Transactional
    public DocResponse attachGroupPhoto(Long docId, MultipartFile photo, Long userId) {
        Document parentDoc = findWithAccessCheck(docId, userId);

        String folder = "docs/%d/photos".formatted(parentDoc.getSite().getId());
        String photoUrl = s3Service.uploadMultipart(photo, folder);

        Document photoDoc = Document.builder()
                .site(parentDoc.getSite())
                .uploader(userRepository.getReferenceById(userId))
                .title("단체 사진 - " + parentDoc.getTitle())
                .type(Document.DocumentType.GROUP_PHOTO)
                .fileUrl(photoUrl)
                .fileSize(photo.getSize())
                .build();

        return DocResponse.from(documentRepository.save(photoDoc));
    }

    // ── 문서 목록 ─────────────────────────────────────────────────────────────

    public Page<DocResponse> list(Long userId, Document.DocumentType type,
                                  Document.DocStatus status, Pageable pageable) {
        return documentRepository
                .findByManagerIdWithFilters(userId, type, status, pageable)
                .map(DocResponse::from);
    }

    // ── PDF URL 조회 ──────────────────────────────────────────────────────────

    public String getPdfUrl(Long docId, Long userId) {
        return findWithAccessCheck(docId, userId).getFileUrl();
    }

    // ── 서명 ─────────────────────────────────────────────────────────────────

    @Transactional
    public SignStatusResponse sign(Long docId, SignRequest request, Long userId) {
        Document doc = findWithAccessCheck(docId, userId);

        Signature signature = Signature.builder()
                .document(doc)
                .signer(userRepository.getReferenceById(userId))
                .signerName(request.getSignerName())
                .signatureData(request.getSignatureData())
                .signedAt(LocalDateTime.now())
                .build();

        signatureRepository.save(signature);
        doc.sign();
        documentRepository.save(doc);

        return buildSignStatus(doc);
    }

    // ── 서명 현황 ─────────────────────────────────────────────────────────────

    public SignStatusResponse getSignStatus(Long docId, Long userId) {
        return buildSignStatus(findWithAccessCheck(docId, userId));
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private Document findWithAccessCheck(Long docId, Long userId) {
        return documentRepository.findByIdAndManagerId(docId, userId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));
    }

    private SignStatusResponse buildSignStatus(Document doc) {
        List<Signature> signatures = signatureRepository.findByDocumentId(doc.getId());
        return SignStatusResponse.builder()
                .documentId(doc.getId())
                .documentStatus(doc.getStatus().name())
                .signatureCount(signatures.size())
                .signatures(signatures.stream().map(SignStatusResponse.SignatureItem::from).toList())
                .build();
    }
}
