package com.yuai.promptgenerator.server.controller;

import java.util.List;

import com.yuai.promptgenerator.core.domain.KnowledgeDocument;
import com.yuai.promptgenerator.core.domain.KnowledgeJob;
import com.yuai.promptgenerator.server.service.KnowledgeAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/admin/knowledge")
public class KnowledgeAdminController {
    private final KnowledgeAdminService service;
    public KnowledgeAdminController(KnowledgeAdminService service) { this.service = service; }

    @PostMapping
    KnowledgeAdminService.UploadResult upload(@RequestParam("file") MultipartFile file) { return service.upload(file); }
    @GetMapping
    List<KnowledgeDocument> list() { return service.list(); }
    @GetMapping("/{id}")
    KnowledgeDocument get(@PathVariable String id) { return service.get(id); }
    @GetMapping("/jobs/{id}")
    KnowledgeJob job(@PathVariable String id) { return service.job(id); }
    @PostMapping("/{id}/reindex")
    KnowledgeJob reindex(@PathVariable String id) { return service.retry(id); }
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
