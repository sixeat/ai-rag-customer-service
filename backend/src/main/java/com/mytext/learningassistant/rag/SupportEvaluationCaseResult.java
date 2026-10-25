package com.mytext.learningassistant.rag;

import java.util.List;

public record SupportEvaluationCaseResult(
    SupportEvaluationCase testCase,
    String status,
    List<String> findings,
    SupportDiagnosticResponse diagnostic
) {}
