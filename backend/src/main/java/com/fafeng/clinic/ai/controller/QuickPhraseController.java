package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.dto.SaveQuickPhraseRequest;
import com.fafeng.clinic.ai.service.QuickPhraseService;
import com.fafeng.clinic.ai.vo.QuickPhraseCleanupVO;
import com.fafeng.clinic.ai.vo.QuickPhraseFieldVO;
import com.fafeng.clinic.ai.vo.QuickPhraseVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quick-phrases")
public class QuickPhraseController {

    private final QuickPhraseService quickPhraseService;

    public QuickPhraseController(QuickPhraseService quickPhraseService) {
        this.quickPhraseService = quickPhraseService;
    }

    @GetMapping("/fields")
    public Result<List<QuickPhraseFieldVO>> fields() {
        return Result.ok(quickPhraseService.listFields());
    }

    @GetMapping("/candidates")
    public Result<List<QuickPhraseVO>> candidates(
            @RequestParam String fieldKey,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(quickPhraseService.listCandidates(fieldKey, limit));
    }

    @GetMapping
    public Result<List<QuickPhraseVO>> list(@RequestParam(required = false) String fieldKey) {
        return Result.ok(quickPhraseService.listManaged(fieldKey));
    }

    @PostMapping
    public Result<QuickPhraseVO> create(@Valid @RequestBody SaveQuickPhraseRequest request) {
        return Result.ok(quickPhraseService.create(request));
    }

    @PutMapping("/{id}")
    public Result<QuickPhraseVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveQuickPhraseRequest request) {
        return Result.ok(quickPhraseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        quickPhraseService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/use")
    public Result<QuickPhraseVO> use(@PathVariable Long id) {
        return Result.ok(quickPhraseService.recordUsage(id));
    }

    @PostMapping("/sync-history")
    public Result<Void> syncHistory() {
        quickPhraseService.syncAllHistory();
        return Result.ok();
    }

    @PostMapping("/cleanup")
    public Result<QuickPhraseCleanupVO> cleanup() {
        return Result.ok(quickPhraseService.cleanupStale());
    }
}
