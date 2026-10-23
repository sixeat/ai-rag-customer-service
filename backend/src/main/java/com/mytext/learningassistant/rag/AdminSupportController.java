package com.mytext.learningassistant.rag;

import java.util.List;

import com.mytext.learningassistant.common.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/support/conversations")
public class AdminSupportController {
    private final SupportService service;

    public AdminSupportController(SupportService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<SupportConversationResponse>> queue() {
        return ApiResponse.ok(service.agentQueue());
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<SupportConversationResponse> reply(@PathVariable long id,
                                                            @Valid @RequestBody SupportMessageRequest request) {
        return ApiResponse.ok(service.agentReply(id, request.content()));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<SupportConversationResponse> close(@PathVariable long id) {
        return ApiResponse.ok(service.close(id));
    }
}
