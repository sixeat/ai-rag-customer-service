package com.mytext.learningassistant.rag;

import java.util.List;

import com.mytext.learningassistant.common.ApiResponse;
import com.mytext.learningassistant.security.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/conversations")
public class SupportController {
    private final SupportService service;
    private final RateLimitService rateLimitService;

    public SupportController(SupportService service, RateLimitService rateLimitService) {
        this.service = service;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ApiResponse<List<SupportConversationResponse>> list(@RequestAttribute("currentUserId") long userId) {
        return ApiResponse.ok(service.listMine(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupportConversationResponse> get(@RequestAttribute("currentUserId") long userId,
                                                        @PathVariable long id) {
        return ApiResponse.ok(service.getMine(userId, id));
    }

    @PostMapping
    public ApiResponse<SupportConversationResponse> create(@RequestAttribute("currentUserId") long userId,
                                                             @Valid @RequestBody SupportMessageRequest request,
                                                             HttpServletRequest httpRequest) {
        rateLimitService.checkRagChat(userId + ":" + httpRequest.getRemoteAddr());
        return ApiResponse.ok(service.send(userId, null, request.content()));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<SupportConversationResponse> send(@RequestAttribute("currentUserId") long userId,
                                                           @PathVariable long id,
                                                           @Valid @RequestBody SupportMessageRequest request,
                                                           HttpServletRequest httpRequest) {
        rateLimitService.checkRagChat(userId + ":" + httpRequest.getRemoteAddr());
        return ApiResponse.ok(service.send(userId, id, request.content()));
    }

    @PostMapping("/{id}/handoff")
    public ApiResponse<SupportConversationResponse> handoff(@RequestAttribute("currentUserId") long userId,
                                                              @PathVariable long id) {
        return ApiResponse.ok(service.requestHuman(userId, id));
    }
}
