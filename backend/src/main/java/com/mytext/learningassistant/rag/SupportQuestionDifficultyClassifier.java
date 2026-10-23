package com.mytext.learningassistant.rag;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SupportQuestionDifficultyClassifier {
    private static final Pattern TOPIC_WORD = Pattern.compile("[\\p{IsHan}]{2,}|[a-z0-9]{3,}");
    public enum Difficulty { LOW, HIGH }

    public Difficulty classify(String question, List<RagSourceResponse> sources) {
        if (question == null || question.isBlank() || sources == null || sources.isEmpty()
            || sources.getFirst().score() == null || sources.getFirst().score() < 0.35) {
            return Difficulty.HIGH;
        }
        String text = question.trim().toLowerCase(Locale.ROOT);
        if (text.length() > 80 || text.chars().filter(character -> character == '?' || character == '？').count() > 1
            || containsAny(text, "如果", "假如", "同时", "分别", "比较", "对比", "区别", "叠加", "例外",
                "冲突", "计算", "赔偿", "退款", "申诉", "封禁", "争议", "风险", "建议", "为什么",
                "怎么办", "影响", "取决于", "多个", "两种", "不同", "条件", "资格",
                "compare", "versus", "refund", "exception", "calculate", "why")) {
            return Difficulty.HIGH;
        }
        return hasTopicalEvidence(text, sources.getFirst()) && containsAny(text, "什么", "何时", "什么时候", "多久", "几点", "几号", "多少", "哪里",
            "在哪", "是否", "能否", "怎么领", "如何领取", "怎么用", "when", "what", "where", "how many")
            ? Difficulty.LOW : Difficulty.HIGH;
    }

    private boolean hasTopicalEvidence(String question, RagSourceResponse source) {
        String topic = question.replaceAll("什么时候|如何领取|怎么用|how many|何时|多久|几点|几号|多少|哪里|在哪|是否|能否|怎么领|什么|when|what|where", " ");
        String evidence = ((source.materialTitle() == null ? "" : source.materialTitle()) + " "
            + (source.excerpt() == null ? "" : source.excerpt())).toLowerCase(Locale.ROOT);
        Set<String> terms = new HashSet<>();
        Matcher matcher = TOPIC_WORD.matcher(topic);
        while (matcher.find()) {
            String word = matcher.group();
            if (Character.UnicodeScript.of(word.codePointAt(0)) == Character.UnicodeScript.HAN) {
                for (int index = 0; index < word.length() - 1; index++) {
                    terms.add(word.substring(index, index + 2));
                }
            } else {
                terms.add(word);
            }
        }
        if (terms.isEmpty()) return false;
        return terms.stream().filter(evidence::contains).count() * 2 >= terms.size();
    }

    private boolean containsAny(String text, String... cues) {
        for (String cue : cues) {
            if (text.contains(cue)) return true;
        }
        return false;
    }
}
