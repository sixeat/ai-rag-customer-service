package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;
import java.util.List;

import com.mytext.learningassistant.common.ApiResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/support-content")
public class AdminSupportContentController {
    private final SupportContentService service;

    public AdminSupportContentController(SupportContentService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<SupportContentResponse>> list() { return ApiResponse.ok(service.list()); }

    @PostMapping
    public ApiResponse<SupportContentResponse> create(@RequestAttribute("currentUserId") long adminId,
            @RequestParam SupportContentCategory category, @RequestParam String title,
            @RequestParam(required = false) String body, @RequestParam(defaultValue = "false") boolean published,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endsAt,
            @RequestParam MultipartFile file) {
        return ApiResponse.ok(service.create(adminId, category, title, body, published, startsAt, endsAt, file));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupportContentResponse> update(@RequestAttribute("currentUserId") long adminId,
            @PathVariable long id, @RequestParam SupportContentCategory category, @RequestParam String title,
            @RequestParam(required = false) String body, @RequestParam(defaultValue = "false") boolean published,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endsAt,
            @RequestParam(required = false) MultipartFile file) {
        return ApiResponse.ok(service.update(adminId, id, category, title, body, published, startsAt, endsAt, file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
