package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.dto.ReorderChannelsRequest;
import com.fafeng.clinic.ai.dto.SaveChatChannelRequest;
import com.fafeng.clinic.ai.dto.SaveEmbeddingChannelRequest;
import com.fafeng.clinic.ai.service.AiChannelService;
import com.fafeng.clinic.ai.vo.ChannelTestResultVO;
import com.fafeng.clinic.ai.vo.ChatChannelVO;
import com.fafeng.clinic.ai.vo.EmbeddingChannelVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/channels")
@RequiredArgsConstructor
public class AiChannelController {

    private final AiChannelService aiChannelService;

    @GetMapping("/chat")
    public Result<List<ChatChannelVO>> listChat() {
        return Result.ok(aiChannelService.listChatChannels());
    }

    @PostMapping("/chat")
    public Result<ChatChannelVO> createChat(@Valid @RequestBody SaveChatChannelRequest request) {
        return Result.ok(aiChannelService.createChatChannel(request));
    }

    @PutMapping("/chat/{channelId}")
    public Result<ChatChannelVO> updateChat(
            @PathVariable String channelId,
            @Valid @RequestBody SaveChatChannelRequest request) {
        return Result.ok(aiChannelService.updateChatChannel(channelId, request));
    }

    @DeleteMapping("/chat/{channelId}")
    public Result<Void> deleteChat(@PathVariable String channelId) {
        aiChannelService.deleteChatChannel(channelId);
        return Result.ok(null);
    }

    @PutMapping("/chat/reorder")
    public Result<Void> reorderChat(@Valid @RequestBody ReorderChannelsRequest request) {
        aiChannelService.reorderChatChannels(request);
        return Result.ok(null);
    }

    @PostMapping("/chat/{channelId}/test")
    public Result<ChannelTestResultVO> testChat(@PathVariable String channelId) {
        return Result.ok(aiChannelService.testChatChannel(channelId));
    }

    @GetMapping("/embedding")
    public Result<List<EmbeddingChannelVO>> listEmbedding() {
        return Result.ok(aiChannelService.listEmbeddingChannels());
    }

    @PostMapping("/embedding")
    public Result<EmbeddingChannelVO> createEmbedding(@Valid @RequestBody SaveEmbeddingChannelRequest request) {
        return Result.ok(aiChannelService.createEmbeddingChannel(request));
    }

    @PutMapping("/embedding/{channelId}")
    public Result<EmbeddingChannelVO> updateEmbedding(
            @PathVariable String channelId,
            @Valid @RequestBody SaveEmbeddingChannelRequest request) {
        return Result.ok(aiChannelService.updateEmbeddingChannel(channelId, request));
    }

    @DeleteMapping("/embedding/{channelId}")
    public Result<Void> deleteEmbedding(@PathVariable String channelId) {
        aiChannelService.deleteEmbeddingChannel(channelId);
        return Result.ok(null);
    }

    @PutMapping("/embedding/reorder")
    public Result<Void> reorderEmbedding(@Valid @RequestBody ReorderChannelsRequest request) {
        aiChannelService.reorderEmbeddingChannels(request);
        return Result.ok(null);
    }

    @PostMapping("/embedding/{channelId}/test")
    public Result<ChannelTestResultVO> testEmbedding(@PathVariable String channelId) {
        return Result.ok(aiChannelService.testEmbeddingChannel(channelId));
    }

    @PostMapping("/import-from-env")
    public Result<Void> importFromEnv() {
        aiChannelService.importFromEnv();
        return Result.ok(null);
    }
}
