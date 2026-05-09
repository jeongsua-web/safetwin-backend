package com.safetwin.publicdata.service;

import com.safetwin.publicdata.dto.AccidentCaseResponse;
import com.safetwin.publicdata.dto.AccidentCaseResponse.CaseItem;
import com.safetwin.publicdata.dto.LawResponse;
import com.safetwin.publicdata.dto.LawResponse.LawItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicDataService {

    private final LawGoKrService lawGoKrService;

    // ── 사고 사례 조회 ────────────────────────────────────────────────────────
    // 고용노동부 공공 API는 개별 사고 사례 서술 형식을 제공하지 않아 큐레이션된 데이터를 사용합니다.

    public AccidentCaseResponse getAccidentCases(String industryType) {
        return AccidentCaseResponse.builder()
                .industryType(industryType)
                .cases(STUB_CASES.getOrDefault(
                        normalizeIndustry(industryType),
                        STUB_CASES.get("기타")))
                .build();
    }

    // ── 법령 검색 ─────────────────────────────────────────────────────────────

    public LawResponse searchLaws(String keyword) {
        List<LawItem> apiResults = lawGoKrService.searchLaws(keyword);
        if (!apiResults.isEmpty()) {
            return LawResponse.builder()
                    .keyword(keyword)
                    .laws(apiResults)
                    .build();
        }

        List<LawItem> matched = ALL_LAWS.stream()
                .filter(law -> matchesKeyword(law, keyword))
                .toList();

        return LawResponse.builder()
                .keyword(keyword)
                .laws(matched.isEmpty() ? ALL_LAWS.subList(0, Math.min(3, ALL_LAWS.size())) : matched)
                .build();
    }

    // ── Stub 데이터 ───────────────────────────────────────────────────────────

    private static final Map<String, List<CaseItem>> STUB_CASES = Map.of(
            "건설", List.of(
                    CaseItem.builder()
                            .title("비계 작업 중 추락 사망사고")
                            .year(2023)
                            .cause("안전난간 미설치 및 안전대 미착용 상태로 2층 비계 작업 중 추락")
                            .law("산업안전보건기준에 관한 규칙 제42조(추락의 방지)")
                            .source("고용노동부 산업재해 현황 2023")
                            .build(),
                    CaseItem.builder()
                            .title("낙하물에 의한 두부 손상 사고")
                            .year(2023)
                            .cause("상부 작업층 자재 낙하, 하부 작업자 안전모 미착용")
                            .law("산업안전보건기준에 관한 규칙 제14조(낙하물에 의한 위험 방지)")
                            .source("고용노동부 산업재해 현황 2023")
                            .build(),
                    CaseItem.builder()
                            .title("굴착기 작업 반경 내 작업자 협착")
                            .year(2022)
                            .cause("중장비 작업 반경 내 작업자 출입, 신호수 미배치")
                            .law("산업안전보건기준에 관한 규칙 제98조(차량계 건설기계의 사용)")
                            .source("고용노동부 산업재해 현황 2022")
                            .build(),
                    CaseItem.builder()
                            .title("전기 작업 중 감전 사고")
                            .year(2022)
                            .cause("임시 배선 절연 불량, 접지 미실시 상태에서 전기 작업")
                            .law("산업안전보건기준에 관한 규칙 제301조(전기 기계·기구 등의 충전부 방호)")
                            .source("고용노동부 산업재해 현황 2022")
                            .build()
            ),
            "제조", List.of(
                    CaseItem.builder()
                            .title("프레스 작업 중 손가락 절단")
                            .year(2023)
                            .cause("프레스 방호장치 미설치, 작업 중 금형 내 이물질 제거 시도")
                            .law("산업안전보건기준에 관한 규칙 제100조(프레스 등의 위험 방지)")
                            .source("고용노동부 산업재해 현황 2023")
                            .build(),
                    CaseItem.builder()
                            .title("컨베이어 롤러부 끼임 사고")
                            .year(2023)
                            .cause("컨베이어 운전 중 롤러부 청소 시도, 방호덮개 제거 상태")
                            .law("산업안전보건기준에 관한 규칙 제92조(컨베이어의 위험 방지)")
                            .source("고용노동부 산업재해 현황 2023")
                            .build(),
                    CaseItem.builder()
                            .title("화학물질 누출로 인한 화학적 화상")
                            .year(2022)
                            .cause("배관 플랜지 볼트 풀림, 강산성 물질 누출 시 보호장구 미착용")
                            .law("산업안전보건기준에 관한 규칙 제439조(관리 대상 유해물질의 취급 기준)")
                            .source("고용노동부 산업재해 현황 2022")
                            .build(),
                    CaseItem.builder()
                            .title("지게차 전도로 인한 협착")
                            .year(2022)
                            .cause("경사면에서 하중 초과 운반 중 지게차 전도, 운전자 협착")
                            .law("산업안전보건기준에 관한 규칙 제179조(화물취급 작업 시의 조치)")
                            .source("고용노동부 산업재해 현황 2022")
                            .build()
            ),
            "기타", List.of(
                    CaseItem.builder()
                            .title("근골격계 질환 - 반복 작업")
                            .year(2023)
                            .cause("단순 반복 작업 및 부적절한 작업 자세로 인한 요추 추간판 탈출증")
                            .law("산업안전보건기준에 관한 규칙 제657조(근골격계 부담 작업의 범위)")
                            .source("고용노동부 산업재해 현황 2023")
                            .build()
            )
    );

    private static final List<LawItem> ALL_LAWS = List.of(
            LawItem.builder()
                    .title("산업안전보건법")
                    .article("제36조")
                    .content("사업주는 건설물, 기계·기구·설비, 원재료, 가스, 증기, 분진 등에 의하거나 작업행동, 그 밖의 업무에 기인하는 유해·위험 요인을 찾아내어 위험성을 결정하고, 그 결과에 따라 이 법과 이 법에 따른 명령에 의한 조치를 하여야 한다.")
                    .category("위험성평가")
                    .build(),
            LawItem.builder()
                    .title("산업안전보건법")
                    .article("제38조")
                    .content("사업주는 다음 각 호의 어느 하나에 해당하는 위험으로 인한 산업재해를 예방하기 위하여 필요한 조치를 하여야 한다.")
                    .category("안전조치")
                    .build(),
            LawItem.builder()
                    .title("산업안전보건법")
                    .article("제29조")
                    .content("사업주는 근로자를 채용할 때와 작업 내용을 변경할 때에는 그 근로자에게 고용노동부령으로 정하는 바에 따라 해당 업무와 관계되는 안전보건교육을 하여야 한다.")
                    .category("안전보건교육")
                    .build(),
            LawItem.builder()
                    .title("중대재해처벌법")
                    .article("제4조")
                    .content("사업주 또는 경영책임자등은 사업주나 법인 또는 기관이 실질적으로 지배·운영·관리하는 사업 또는 사업장에서 종사자의 안전·보건상 유해 또는 위험을 방지하기 위하여 필요한 조치를 하여야 한다.")
                    .category("사업주 의무")
                    .build(),
            LawItem.builder()
                    .title("산업안전보건기준에 관한 규칙")
                    .article("제32조")
                    .content("사업주는 다음 각 호의 어느 하나에 해당하는 작업을 하는 근로자에게는 다음 각 호의 구분에 따른 보호구를 지급하고 착용하도록 하여야 한다. (안전모, 안전대 등)")
                    .category("보호구")
                    .build(),
            LawItem.builder()
                    .title("산업안전보건기준에 관한 규칙")
                    .article("제42조")
                    .content("사업주는 근로자가 추락하거나 넘어질 위험이 있는 장소 또는 기계·설비·선박블록 등에서 작업을 할 때에 추락하거나 넘어질 위험이 있는 장소에는 안전난간, 울타리, 수직형 추락방망 또는 덮개 등의 방호 조치를 충분한 강도를 가진 구조로 튼튼하게 설치하여야 한다.")
                    .category("추락방지")
                    .build(),
            LawItem.builder()
                    .title("산업안전보건기준에 관한 규칙")
                    .article("제14조")
                    .content("사업주는 작업장으로 통하는 장소 또는 작업장 내에서 근로자에게 위험을 미칠 우려가 있는 물체가 떨어지거나 날아올 위험이 있는 경우에는 그 위험을 방지하기 위하여 필요한 조치를 하여야 한다.")
                    .category("낙하물 방지")
                    .build()
    );

    private String normalizeIndustry(String industryType) {
        if (industryType == null) return "기타";
        String lower = industryType.toLowerCase();
        if (lower.contains("건설") || lower.contains("construction")) return "건설";
        if (lower.contains("제조") || lower.contains("manufacturing")) return "제조";
        return "기타";
    }

    private boolean matchesKeyword(LawItem law, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String lk = keyword.toLowerCase();
        return law.getTitle().toLowerCase().contains(lk)
                || law.getArticle().toLowerCase().contains(lk)
                || law.getContent().toLowerCase().contains(lk)
                || law.getCategory().toLowerCase().contains(lk);
    }
}
