package com.safetwin.docs.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.safetwin.entity.Analysis;
import com.safetwin.entity.Risk;
import com.safetwin.entity.Site;
import com.safetwin.entity.Worker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class PdfGenerationService {

    private static final DeviceRgb HEADER_BG = new DeviceRgb(41, 82, 163);
    private static final DeviceRgb ROW_ALT_BG = new DeviceRgb(245, 247, 250);
    private static final DeviceRgb HIGH_COLOR = new DeviceRgb(220, 53, 69);
    private static final DeviceRgb MEDIUM_COLOR = new DeviceRgb(255, 153, 0);
    private static final DeviceRgb LOW_COLOR = new DeviceRgb(40, 167, 69);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── 위험성 평가서 ─────────────────────────────────────────────────────────

    public byte[] generateRiskAssessment(Analysis analysis, List<Risk> risks) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            PdfFont font = loadFont();
            PdfFont bold = loadFont();

            Site site = analysis.getZone().getSite();
            String analyzedAt = analysis.getUpdatedAt() != null
                    ? analysis.getUpdatedAt().format(DATETIME_FMT) : "-";

            // 제목
            doc.add(new Paragraph("위험성 평가서")
                    .setFont(bold).setFontSize(22)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // 문서 메타 정보
            doc.add(buildInfoTable(font, bold, site, analyzedAt, analysis));

            doc.add(new Paragraph("\n위험 요소 목록")
                    .setFont(bold).setFontSize(14).setMarginTop(20).setMarginBottom(8));

            // 위험 요소 테이블
            doc.add(buildRiskTable(font, bold, risks));

            // 법적 고지
            doc.add(new Paragraph(
                    "\n※ 본 평가서는 산업안전보건법 제36조에 따라 작성된 법적 증빙 서류입니다.")
                    .setFont(font).setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginTop(30));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Risk assessment PDF generation failed", e);
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    // ── 교육 확인서 ───────────────────────────────────────────────────────────

    public byte[] generateEducationCert(Site site, String educationDate,
                                        String content, List<Worker> workers) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(50, 50, 50, 50);

            PdfFont font = loadFont();
            PdfFont bold = loadFont();

            // 제목
            doc.add(new Paragraph("안전보건교육 확인서")
                    .setFont(bold).setFontSize(22)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // 교육 정보
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));
            addInfoRow(infoTable, font, bold, "사업장명", site.getName());
            addInfoRow(infoTable, font, bold, "소재지", site.getAddress() != null ? site.getAddress() : "-");
            addInfoRow(infoTable, font, bold, "교육 일시", educationDate);
            doc.add(infoTable);

            doc.add(new Paragraph("\n교육 내용")
                    .setFont(bold).setFontSize(13).setMarginTop(16).setMarginBottom(8));
            doc.add(new Paragraph(content)
                    .setFont(font).setFontSize(11)
                    .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                    .setPadding(10));

            doc.add(new Paragraph("\n교육 참여자 서명 현황")
                    .setFont(bold).setFontSize(13).setMarginTop(16).setMarginBottom(8));
            doc.add(buildWorkerTable(font, bold, workers));

            // 확인 서명란
            doc.add(new Paragraph("\n\n교육 담당자 서명: ___________________     일자: ___________")
                    .setFont(font).setFontSize(11).setTextAlignment(TextAlignment.RIGHT));

            doc.add(new Paragraph(
                    "\n※ 본 확인서는 산업안전보건법 제29조에 의거한 안전보건교육 이수 증빙 서류입니다.")
                    .setFont(font).setFontSize(9)
                    .setFontColor(ColorConstants.GRAY));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Education cert PDF generation failed", e);
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    // ── 내부 빌더 헬퍼 ───────────────────────────────────────────────────────

    private Table buildInfoTable(PdfFont font, PdfFont bold,
                                 Site site, String analyzedAt, Analysis analysis) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100));

        addInfoRow(table, font, bold, "사업장명", site.getName());
        addInfoRow(table, font, bold, "소재지", site.getAddress() != null ? site.getAddress() : "-");
        addInfoRow(table, font, bold, "사업자번호", site.getBizNumber() != null ? site.getBizNumber() : "-");
        addInfoRow(table, font, bold, "평가 구역", analysis.getZone().getName());
        addInfoRow(table, font, bold, "분석 일시", analyzedAt);
        addInfoRow(table, font, bold, "종합 점수",
                analysis.getOverallScore() != null ? analysis.getOverallScore() + "점" : "-");
        return table;
    }

    private void addInfoRow(Table table, PdfFont font, PdfFont bold, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(bold).setFontSize(10))
                .setBackgroundColor(ROW_ALT_BG).setPadding(6));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(10))
                .setPadding(6));
    }

    private Table buildRiskTable(PdfFont font, PdfFont bold, List<Risk> risks) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 20, 30, 10, 20, 15}))
                .setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"#", "위험 요소", "상세 설명", "위험도", "법적 근거", "조치사항"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(10)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG).setPadding(6));
        }

        for (int i = 0; i < risks.size(); i++) {
            Risk risk = risks.get(i);
            boolean alt = i % 2 == 1;

            table.addCell(cellOf(font, String.valueOf(i + 1), alt));
            table.addCell(cellOf(font, nvl(risk.getLabel()), alt));
            table.addCell(cellOf(font, nvl(risk.getDescription()), alt));

            DeviceRgb levelColor = switch (risk.getLevel()) {
                case HIGH, CRITICAL -> HIGH_COLOR;
                case MEDIUM -> MEDIUM_COLOR;
                case LOW -> LOW_COLOR;
            };
            table.addCell(new Cell()
                    .add(new Paragraph(risk.getLevel().name()).setFont(bold).setFontSize(10)
                            .setFontColor(levelColor))
                    .setBackgroundColor(alt ? ROW_ALT_BG : null).setPadding(6));

            table.addCell(cellOf(font, nvl(risk.getLaw()), alt));
            table.addCell(cellOf(font, nvl(risk.getAction()), alt));
        }

        if (risks.isEmpty()) {
            table.addCell(new Cell(1, 6)
                    .add(new Paragraph("탐지된 위험 요소가 없습니다.").setFont(font).setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(12));
        }
        return table;
    }

    private Table buildWorkerTable(PdfFont font, PdfFont bold, List<Worker> workers) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 25, 25, 20, 20}))
                .setWidth(UnitValue.createPercentValue(100));

        String[] headers = {"#", "성명", "직종", "연락처", "서명"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(10)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(HEADER_BG).setPadding(6));
        }

        for (int i = 0; i < workers.size(); i++) {
            Worker w = workers.get(i);
            boolean alt = i % 2 == 1;
            table.addCell(cellOf(font, String.valueOf(i + 1), alt));
            table.addCell(cellOf(font, nvl(w.getName()), alt));
            table.addCell(cellOf(font, nvl(w.getOccupation()), alt));
            table.addCell(cellOf(font, nvl(w.getPhone()), alt));
            table.addCell(new Cell()
                    .add(new Paragraph("               ").setFont(font).setFontSize(10))
                    .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1))
                    .setBackgroundColor(alt ? ROW_ALT_BG : null).setPadding(6));
        }
        return table;
    }

    private Cell cellOf(PdfFont font, String text, boolean alt) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBackgroundColor(alt ? ROW_ALT_BG : null).setPadding(6);
    }

    private String nvl(String s) {
        return s != null ? s : "-";
    }

    /**
     * 한글 폰트 로딩.
     * NanumGothic.ttf 를 src/main/resources/fonts/ 에 위치시키면 한글이 정상 출력됩니다.
     * 폰트 파일이 없을 경우 Helvetica(영문 전용)로 폴백됩니다.
     */
    private PdfFont loadFont() {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/NanumGothic.ttf");
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            }
        } catch (Exception e) {
            log.warn("Korean font not found at /fonts/NanumGothic.ttf, falling back to Helvetica");
        }
        try {
            return PdfFontFactory.createFont();
        } catch (Exception e) {
            throw new RuntimeException("Font loading failed", e);
        }
    }
}
