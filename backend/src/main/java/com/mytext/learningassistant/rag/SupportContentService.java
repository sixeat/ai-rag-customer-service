package com.mytext.learningassistant.rag;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mytext.learningassistant.common.BusinessException;
import com.mytext.learningassistant.material.LearningMaterialEntity;
import com.mytext.learningassistant.material.LearningMaterialRepository;
import com.mytext.learningassistant.material.MaterialService;
import com.mytext.learningassistant.material.MaterialTextStatus;
import com.mytext.learningassistant.material.UpdateMaterialRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupportContentService {
    private final SupportContentRepository contents;
    private final SupportRetiredMaterialRepository retiredMaterials;
    private final LearningMaterialRepository materials;
    private final MaterialService materialService;

    public SupportContentService(SupportContentRepository contents, SupportRetiredMaterialRepository retiredMaterials,
                                 LearningMaterialRepository materials, MaterialService materialService) {
        this.contents = contents;
        this.retiredMaterials = retiredMaterials;
        this.materials = materials;
        this.materialService = materialService;
    }

    @Transactional(readOnly = true)
    public List<SupportContentResponse> list() {
        return contents.findAllByOrderByUpdatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional
    public SupportContentResponse create(long adminId, SupportContentCategory category, String title, String body,
                                         boolean published, LocalDateTime startsAt, LocalDateTime endsAt,
                                         MultipartFile file) {
        validate(category, title, body, startsAt, endsAt, file, true);
        long materialId = materialService.upload(adminId, title.trim(), null, file, null).id();
        SupportContentEntity content = new SupportContentEntity();
        content.setOwnerId(adminId);
        content.setMaterialId(materialId);
        apply(content, category, title, body, published, startsAt, endsAt);
        return response(contents.save(content));
    }

    @Transactional
    public SupportContentResponse update(long adminId, long id, SupportContentCategory category, String title,
                                         String body, boolean published, LocalDateTime startsAt,
                                         LocalDateTime endsAt, MultipartFile file) {
        SupportContentEntity content = require(id);
        validate(category, title, body, startsAt, endsAt, file, false);
        if (file != null && !file.isEmpty()) {
            long oldMaterialId = content.getMaterialId();
            long newMaterialId = materialService.upload(adminId, title.trim(), null, file, null).id();
            retiredMaterials.save(new SupportRetiredMaterialEntity(oldMaterialId, content.getCategory().scenario()));
            content.setMaterialId(newMaterialId);
            content.setOwnerId(adminId);
        } else if (!title.trim().equals(content.getTitle())) {
            materialService.update(content.getOwnerId(), content.getMaterialId(),
                new UpdateMaterialRequest(title.trim(), null));
        }
        apply(content, category, title, file == null || file.isEmpty() ? content.getBody() : body,
            published, startsAt, endsAt);
        return response(contents.save(content));
    }

    @Transactional
    public void delete(long id) {
        SupportContentEntity content = require(id);
        retiredMaterials.save(new SupportRetiredMaterialEntity(content.getMaterialId(), content.getCategory().scenario()));
        contents.delete(content);
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialEntity> activeMaterials(KnowledgeBaseScenario scenario) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        return contents.findByCategoryInAndPublishedTrue(categories(scenario)).stream()
            .filter(content -> (content.getStartsAt() == null || !content.getStartsAt().isAfter(now))
                && (content.getEndsAt() == null || content.getEndsAt().isAfter(now)))
            .map(content -> materials.findByIdAndOwnerId(content.getMaterialId(), content.getOwnerId()).orElse(null))
            .filter(material -> material != null && (material.getTextStatus() == MaterialTextStatus.READY
                || material.getTextStatus() == MaterialTextStatus.PARTIAL))
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> diagnosticFilterCounts(KnowledgeBaseScenario scenario) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        Map<String, Long> counts = contents.findAllByOrderByUpdatedAtDesc().stream()
            .filter(content -> categories(scenario).contains(content.getCategory()))
            .collect(Collectors.groupingBy(content -> {
                if (!content.isPublished()) return "DRAFT";
                if (content.getStartsAt() != null && content.getStartsAt().isAfter(now)) return "FUTURE";
                if (content.getEndsAt() != null && !content.getEndsAt().isAfter(now)) return "EXPIRED";
                return materials.findByIdAndOwnerId(content.getMaterialId(), content.getOwnerId())
                    .filter(material -> material.getTextStatus() == MaterialTextStatus.READY
                        || material.getTextStatus() == MaterialTextStatus.PARTIAL)
                    .isPresent() ? "ACTIVE" : "NOT_READY";
            }, Collectors.counting()));
        long retiredCount = retiredMaterials.countByScenario(scenario);
        if (retiredCount > 0) counts.put("RETIRED", retiredCount);
        return counts;
    }

    @Transactional(readOnly = true)
    public Optional<SupportContentEntity> matchingExpired(KnowledgeBaseScenario scenario, String question,
                                                           List<LearningMaterialEntity> activeMaterials) {
        String normalizedQuestion = normalizeTopic(question);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        return contents.findByCategoryInAndPublishedTrue(categories(scenario)).stream()
            .filter(content -> matchesTitle(normalizedQuestion, content.getTitle()))
            .max(Comparator.comparingInt((SupportContentEntity content) -> normalizeTopic(content.getTitle()).length())
                .thenComparing(content -> content.getEndsAt() == null ? LocalDateTime.MAX : content.getEndsAt()))
            .filter(content -> content.getEndsAt() != null && !content.getEndsAt().isAfter(now))
            .filter(content -> activeMaterials.stream().noneMatch(material ->
                normalizeTopic(content.getTitle()).equals(normalizeTopic(material.getTitle()))))
            .filter(content -> materials.findByIdAndOwnerId(content.getMaterialId(), content.getOwnerId())
                .filter(material -> material.getTextStatus() == MaterialTextStatus.READY
                    || material.getTextStatus() == MaterialTextStatus.PARTIAL).isPresent());
    }

    @Transactional(readOnly = true)
    public List<LearningMaterialEntity> eligibleBaseMaterials(KnowledgeBaseScenario scenario,
                                                                List<LearningMaterialEntity> baseMaterials) {
        if (baseMaterials.isEmpty()) return baseMaterials;
        Set<Long> activeIds = activeMaterials(scenario).stream()
            .map(LearningMaterialEntity::getId).collect(Collectors.toSet());
        Set<Long> managedIds = contents.findByMaterialIdIn(baseMaterials.stream()
            .map(LearningMaterialEntity::getId).toList()).stream()
            .map(SupportContentEntity::getMaterialId).collect(Collectors.toSet());
        Set<Long> retiredIds = retiredMaterials.findByMaterialIdIn(baseMaterials.stream()
            .map(LearningMaterialEntity::getId).toList()).stream()
            .map(SupportRetiredMaterialEntity::getMaterialId).collect(Collectors.toSet());
        return baseMaterials.stream()
            .filter(material -> !retiredIds.contains(material.getId()))
            .filter(material -> !managedIds.contains(material.getId()) || activeIds.contains(material.getId()))
            .toList();
    }

    private boolean matchesTitle(String normalizedQuestion, String title) {
        String normalizedTitle = normalizeTopic(title);
        return normalizedTitle.length() >= 6 && normalizedQuestion.contains(normalizedTitle);
    }

    private List<SupportContentCategory> categories(KnowledgeBaseScenario scenario) {
        return switch (scenario) {
            case ECOMMERCE -> List.of(SupportContentCategory.ECOMMERCE_PROMOTION);
            case GAME -> List.of(SupportContentCategory.GAME_EVENT, SupportContentCategory.GAME_UPDATE);
            case GENERAL -> List.of();
        };
    }

    private String normalizeTopic(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
            .replace("演示", "").replace("的", "")
            .replaceAll("[^\\p{IsHan}a-z0-9]", "");
    }

    private void validate(SupportContentCategory category, String title, String body,
                          LocalDateTime startsAt, LocalDateTime endsAt, MultipartFile file, boolean requiredFile) {
        if (category == null || title == null || title.isBlank() || title.trim().length() > 128) {
            throw new BusinessException(400, "请选择内容类型并填写 128 字以内的标题");
        }
        if (body != null && body.length() > 100_000) {
            throw new BusinessException(400, "正文不能超过 100000 字");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
        if (requiredFile && (file == null || file.isEmpty())) {
            throw new BusinessException(400, "请填写正文或上传资料文件");
        }
    }

    private void apply(SupportContentEntity content, SupportContentCategory category, String title, String body,
                       boolean published, LocalDateTime startsAt, LocalDateTime endsAt) {
        content.setCategory(category);
        content.setTitle(title.trim());
        content.setBody(body == null || body.isBlank() ? null : body.trim());
        content.setPublished(published);
        content.setStartsAt(startsAt);
        content.setEndsAt(endsAt);
    }

    private SupportContentEntity require(long id) {
        return contents.findById(id).orElseThrow(() -> new BusinessException(404, "客服知识不存在"));
    }

    private SupportContentResponse response(SupportContentEntity content) {
        String status = materials.findByIdAndOwnerId(content.getMaterialId(), content.getOwnerId())
            .map(material -> material.getTextStatus().name()).orElse("MISSING");
        return new SupportContentResponse(content.getId(), content.getMaterialId(), content.getCategory(),
            content.getTitle(), content.getBody(), content.isPublished(), content.getStartsAt(),
            content.getEndsAt(), status, content.getUpdatedAt());
    }
}
