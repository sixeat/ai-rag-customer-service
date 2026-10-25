package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.mytext.learningassistant.common.BusinessException;

import org.springframework.stereotype.Service;

/** Fixed interview-demo regression suite. Each case runs separately to show progress and limit request time. */
@Service
public class SupportEvaluationService {
    private static final String EXPIRED = "周年庆双倍返利";
    private static final String EVENT = "秋日签到";
    private static final String UPDATE = "版本更新";
    private static final String COMMERCE = "周末优惠";
    private static final List<SupportEvaluationCase> CASES = List.of(
        expired("E01", "游戏周年庆双倍返利活动现在还能参加吗？"),
        expired("E02", "游戏周年庆双倍返利什么时候结束？"),
        expired("E03", "游戏周年庆双倍返利还有奖励可以领吗？"),
        expired("E04", "游戏周年庆双倍返利能补领吗？"),
        expired("E05", "游戏周年庆双倍返利是否延期？"),
        expired("E06", "游戏周年庆双倍返利活动规则是什么？"),
        expired("E07", "游戏周年庆双倍返利是昨天结束的吗？"),
        expired("E08", "游戏周年庆双倍返利几点关闭？"),
        expired("E09", "游戏周年庆双倍返利为什么看不到入口？"),
        expired("E10", "游戏周年庆双倍返利现在还能按旧规则充值吗？"),
        answer("G01", "游戏活动", "游戏《叫我万岁爷》秋日签到活动什么时候结束？",
            KnowledgeBaseScenario.GAME, "LOW", EVENT, "2026-11-07"),
        answer("G02", "游戏活动", "游戏《叫我万岁爷》秋日签到活动每天能签到几次？",
            KnowledgeBaseScenario.GAME, null, EVENT, "一次"),
        answer("G03", "游戏活动", "游戏《叫我万岁爷》秋日签到活动累计签到3天有什么奖励？",
            KnowledgeBaseScenario.GAME, null, EVENT, "秋日礼盒"),
        answer("G04", "游戏活动", "游戏《叫我万岁爷》秋日签到活动累计签到7天有什么奖励？",
            KnowledgeBaseScenario.GAME, null, EVENT, "秋日头像框"),
        answer("G05", "游戏活动", "游戏《叫我万岁爷》秋日签到活动漏签能补吗？",
            KnowledgeBaseScenario.GAME, null, EVENT, "不能"),
        answer("G06", "游戏活动", "游戏《叫我万岁爷》秋日签到活动如果漏签，3天和7天奖励分别怎样计算？",
            KnowledgeBaseScenario.GAME, "HIGH", EVENT, "漏签"),
        answer("V01", "版本更新", "游戏《我在大清当皇帝》演示版本号是什么？",
            KnowledgeBaseScenario.GAME, null, UPDATE, "v9.9.0-demo"),
        answer("V02", "版本更新", "游戏《我在大清当皇帝》版本更新新增了什么入口？",
            KnowledgeBaseScenario.GAME, null, UPDATE, "活动日历"),
        answer("V03", "版本更新", "游戏《我在大清当皇帝》版本更新优化了什么？",
            KnowledgeBaseScenario.GAME, null, UPDATE, "邮件"),
        answer("V04", "版本更新", "游戏《我在大清当皇帝》版本更新修复了哪类设备问题？",
            KnowledgeBaseScenario.GAME, null, UPDATE, "安卓"),
        answer("V05", "版本更新", "游戏《我在大清当皇帝》版本更新后仍显示旧内容怎么办？",
            KnowledgeBaseScenario.GAME, "HIGH", UPDATE, "重新登录"),
        answer("C01", "电商优惠", "演示商城周末优惠什么时候结束？",
            KnowledgeBaseScenario.ECOMMERCE, "LOW", COMMERCE, "2026-11-07"),
        answer("C02", "电商优惠", "演示商城周末优惠哪些商品参加？",
            KnowledgeBaseScenario.ECOMMERCE, null, COMMERCE, "旅行水杯"),
        answer("C03", "电商优惠", "演示商城旅行水杯买120元能减多少？",
            KnowledgeBaseScenario.ECOMMERCE, null, COMMERCE, "15"),
        answer("C04", "电商优惠", "演示商城周末优惠的运费计入满100元门槛吗？",
            KnowledgeBaseScenario.ECOMMERCE, null, COMMERCE, "不计"),
        answer("C05", "电商优惠", "演示商城周末优惠能和其他满减券叠加吗？",
            KnowledgeBaseScenario.ECOMMERCE, null, COMMERCE, "不能"),
        answer("C06", "电商优惠", "演示商城周末优惠如果水杯120元且运费10元，还能叠加满减券吗，实付怎么算？",
            KnowledgeBaseScenario.ECOMMERCE, "HIGH", COMMERCE, "15"),
        handoff("H01", "转人工"),
        handoff("H02", "请帮我转人工"),
        handoff("H03", "我要找人工客服")
    );

    private final SupportDiagnosticsService diagnostics;
    private final SupportContentService content;

    public SupportEvaluationService(SupportDiagnosticsService diagnostics, SupportContentService content) {
        this.diagnostics = diagnostics;
        this.content = content;
    }

    public List<SupportEvaluationCase> cases() { return CASES; }

    public SupportEvaluationCaseResult runCase(long adminId, String id) {
        SupportEvaluationCase testCase = CASES.stream().filter(item -> item.id().equals(id))
            .findFirst().orElseThrow(() -> new BusinessException(404, "评估题目不存在"));
        if (testCase.requiredTitle() != null && !hasRequiredContent(testCase)) {
            return new SupportEvaluationCaseResult(testCase, "SKIPPED",
                List.of("请先按 samples/support-knowledge/README.md 上传并发布对应演示资料，设置正确有效期。"), null);
        }
        SupportDiagnosticResponse result = diagnostics.diagnose(
            adminId, new SupportDiagnosticRequest(testCase.question(), null));
        return score(testCase, result);
    }

    SupportEvaluationCaseResult score(SupportEvaluationCase testCase, SupportDiagnosticResponse result) {
        List<String> findings = new ArrayList<>();
        if (testCase.expectedScenario() != null && testCase.expectedScenario() != result.scenario()) {
            findings.add("场景不符：预期 " + testCase.expectedScenario() + "，实际 " + result.scenario());
        }
        if (!testCase.expectedOutcome().equals(result.outcome())) {
            findings.add("结果不符：预期 " + testCase.expectedOutcome() + "，实际 " + result.outcome());
        }
        if (testCase.expectedDifficulty() != null && "ANSWERED".equals(result.outcome())
            && !testCase.expectedDifficulty().equals(result.difficulty())) {
            findings.add("难度分级不符：预期 " + testCase.expectedDifficulty() + "，实际 " + result.difficulty());
        }
        if (testCase.expectedSourceTitle() != null && result.sources().stream()
            .noneMatch(source -> source.materialTitle() != null
                && source.materialTitle().contains(testCase.expectedSourceTitle()))) {
            findings.add("未引用预期资料：" + testCase.expectedSourceTitle());
        }
        if (testCase.mustContain() != null && !normalize(result.answer()).contains(normalize(testCase.mustContain()))) {
            findings.add("回答缺少关键事实：" + testCase.mustContain());
        }
        return new SupportEvaluationCaseResult(testCase, findings.isEmpty() ? "PASS" : "FAIL", findings, result);
    }

    private boolean hasRequiredContent(SupportEvaluationCase testCase) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        return content.list().stream().anyMatch(item ->
            item.category().scenario() == testCase.expectedScenario()
                && item.title().contains(testCase.requiredTitle())
                && item.published()
                && ("READY".equals(item.textStatus()) || "PARTIAL".equals(item.textStatus()))
                && ("EXPIRED_NOTICE".equals(testCase.expectedOutcome())
                    ? item.endsAt() != null && !item.endsAt().isAfter(now)
                    : (item.startsAt() == null || !item.startsAt().isAfter(now))
                        && (item.endsAt() == null || item.endsAt().isAfter(now))));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", "");
    }

    private static SupportEvaluationCase expired(String id, String question) {
        return new SupportEvaluationCase(id, "过期陷阱", question, KnowledgeBaseScenario.GAME,
            "EXPIRED_NOTICE", null, EXPIRED, EXPIRED, "已于2026-10-15");
    }

    private static SupportEvaluationCase answer(String id, String group, String question,
                                                 KnowledgeBaseScenario scenario, String difficulty,
                                                 String title, String fact) {
        return new SupportEvaluationCase(id, group, question, scenario, "ANSWERED", difficulty,
            title, title, fact);
    }

    private static SupportEvaluationCase handoff(String id, String question) {
        return new SupportEvaluationCase(id, "主动转人工", question, null,
            "HANDOFF_REQUESTED", null, null, null, null);
    }
}
