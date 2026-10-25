package com.mytext.learningassistant.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SupportEvaluationServiceTest {
    private final SupportDiagnosticsService diagnostics = mock(SupportDiagnosticsService.class);
    private final SupportContentService content = mock(SupportContentService.class);
    private final SupportEvaluationService service = new SupportEvaluationService(diagnostics, content);

    @Test
    void bundledSuiteContainsThirtyCasesAndSkipsMissingKnowledge() {
        assertThat(service.cases()).hasSize(30);
        when(content.list()).thenReturn(List.of());

        SupportEvaluationCaseResult result = service.runCase(1L, "E01");

        assertThat(result.status()).isEqualTo("SKIPPED");
        verify(diagnostics, never()).diagnose(anyLong(), any());
    }

    @Test
    void directHandoffCasePassesWithoutKnowledge() {
        SupportDiagnosticResponse handoff = new SupportDiagnosticResponse(
            "转人工", null, "HANDOFF_REQUESTED", "用户主动请求转人工。", List.of(),
            0, 0, Map.of(), null, null, null, null, false, null, 1);
        when(diagnostics.diagnose(eq(1L), any())).thenReturn(handoff);

        assertThat(service.runCase(1L, "H01").status()).isEqualTo("PASS");
    }

    @Test
    void expiredCaseFailsIfOldRuleIsAnsweredInsteadOfEndNotice() {
        SupportEvaluationCase testCase = service.cases().getFirst();
        SupportDiagnosticResponse stale = new SupportDiagnosticResponse(
            testCase.question(), KnowledgeBaseScenario.GAME, "ANSWERED",
            "旧活动返利仍可领取。", List.of(), 1, 1, Map.of(),
            "LOW", "SMALL", "SMALL", "small", false, 20, 10);

        SupportEvaluationCaseResult result = service.score(testCase, stale);

        assertThat(result.status()).isEqualTo("FAIL");
        assertThat(result.findings()).anyMatch(value -> value.contains("结果不符"));
    }
}
