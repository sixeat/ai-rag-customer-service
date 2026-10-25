package com.mytext.learningassistant.rag;

import java.util.List;

import com.mytext.learningassistant.common.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportDiagnosticsController {
    private final SupportDiagnosticsService diagnostics;
    private final SupportEvaluationService evaluation;

    public AdminSupportDiagnosticsController(SupportDiagnosticsService diagnostics,
                                              SupportEvaluationService evaluation) {
        this.diagnostics = diagnostics;
        this.evaluation = evaluation;
    }

    @PostMapping("/diagnose")
    public ApiResponse<SupportDiagnosticResponse> diagnose(@RequestAttribute("currentUserId") long adminId,
                                                             @Valid @RequestBody SupportDiagnosticRequest request) {
        return ApiResponse.ok(diagnostics.diagnose(adminId, request));
    }

    @GetMapping("/evaluation/cases")
    public ApiResponse<List<SupportEvaluationCase>> cases() {
        return ApiResponse.ok(evaluation.cases());
    }

    @PostMapping("/evaluation/cases/{id}/run")
    public ApiResponse<SupportEvaluationCaseResult> runCase(@RequestAttribute("currentUserId") long adminId,
                                                              @PathVariable String id) {
        return ApiResponse.ok(evaluation.runCase(adminId, id));
    }
}
