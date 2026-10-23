package com.mytext.learningassistant.rag;

import com.mytext.learningassistant.common.ApiResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/knowledge-bases")
public class AdminKnowledgeBaseController {
    private final KnowledgeBaseService service;

    public AdminKnowledgeBaseController(KnowledgeBaseService service) {
        this.service = service;
    }

    @PutMapping("/{id}/publish")
    public ApiResponse<KnowledgeBaseResponse> publish(@RequestAttribute("currentUserId") long adminId,
                                                       @PathVariable long id,
                                                       @RequestParam boolean published) {
        return ApiResponse.ok(service.publish(adminId, id, published));
    }
}
