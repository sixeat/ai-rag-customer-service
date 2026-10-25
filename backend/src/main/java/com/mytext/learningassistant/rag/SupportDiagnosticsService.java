package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mytext.learningassistant.common.BusinessException;
import com.mytext.learningassistant.llm.ThirdPartyLlmClient;

import org.springframework.stereotype.Service;

@Service
public class SupportDiagnosticsService {
    private final KnowledgeBaseService knowledgeBases;
    private final ThirdPartyLlmClient llmClient;

    public SupportDiagnosticsService(KnowledgeBaseService knowledgeBases, ThirdPartyLlmClient llmClient) {
        this.knowledgeBases = knowledgeBases;
        this.llmClient = llmClient;
    }

    public SupportDiagnosticResponse diagnose(long adminId, SupportDiagnosticRequest request) {
        long started = System.nanoTime();
        String question = request.question().trim();
        if (SupportService.requestsHuman(question)) {
            return shortCircuit(question, null, "HANDOFF_REQUESTED", "用户主动请求转人工。", started);
        }
        KnowledgeBaseScenario scenario = request.scenario();
        if (scenario == KnowledgeBaseScenario.GENERAL) {
            throw new BusinessException(400, "诊断场景只能是电商或游戏");
        }
        if (scenario == null) {
            scenario = SupportService.route(question);
            if (scenario == null) {
                scenario = llmClient.classifySupportScenario(adminId, question)
                    .filter(value -> !"UNKNOWN".equals(value))
                    .map(KnowledgeBaseScenario::valueOf).orElse(null);
            }
        }
        if (scenario == null) {
            return shortCircuit(question, null, "NEEDS_SCENARIO",
                "请说明是电商问题还是游戏问题。", started);
        }
        SupportDiagnosticResponse result = knowledgeBases.diagnoseScenario(adminId, scenario, question);
        return new SupportDiagnosticResponse(result.question(), result.scenario(), result.outcome(),
            result.answer(), result.sources(), result.activeMaterials(), result.candidateChunks(),
            result.contentFilters(), result.difficulty(), result.requestedTier(), result.actualTier(),
            result.modelName(), result.fallback(), result.totalTokens(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }

    private SupportDiagnosticResponse shortCircuit(String question, KnowledgeBaseScenario scenario,
                                                    String outcome, String answer, long started) {
        return new SupportDiagnosticResponse(question, scenario, outcome, answer, List.of(), 0, 0,
            Map.of(), null, null, null, null, false, null,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }
}
