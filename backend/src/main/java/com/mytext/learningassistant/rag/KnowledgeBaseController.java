package com.mytext.learningassistant.rag;

import java.util.List;

import com.mytext.learningassistant.common.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {
    private final KnowledgeBaseService service;

    public KnowledgeBaseController(KnowledgeBaseService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> list(@RequestAttribute("currentUserId") long userId) {
        return ApiResponse.ok(service.list(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> get(@RequestAttribute("currentUserId") long userId, @PathVariable long id) {
        return ApiResponse.ok(service.get(userId, id));
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(@RequestAttribute("currentUserId") long userId,
                                                       @Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(service.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseResponse> update(@RequestAttribute("currentUserId") long userId,
                                                       @PathVariable long id,
                                                       @Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(service.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestAttribute("currentUserId") long userId, @PathVariable long id) {
        service.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/materials/{materialId}")
    public ApiResponse<KnowledgeBaseResponse> addMaterial(@RequestAttribute("currentUserId") long userId,
                                                            @PathVariable long id, @PathVariable long materialId) {
        return ApiResponse.ok(service.addMaterial(userId, id, materialId));
    }

    @DeleteMapping("/{id}/materials/{materialId}")
    public ApiResponse<KnowledgeBaseResponse> removeMaterial(@RequestAttribute("currentUserId") long userId,
                                                               @PathVariable long id, @PathVariable long materialId) {
        return ApiResponse.ok(service.removeMaterial(userId, id, materialId));
    }

    @PostMapping("/{id}/ask")
    public ApiResponse<KnowledgeBaseAnswer> ask(@RequestAttribute("currentUserId") long userId,
                                                  @PathVariable long id,
                                                  @Valid @RequestBody KnowledgeBaseAskRequest request) {
        return ApiResponse.ok(service.ask(userId, id, request));
    }
}
