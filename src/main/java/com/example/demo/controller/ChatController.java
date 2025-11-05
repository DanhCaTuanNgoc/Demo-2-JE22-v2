package com.example.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.model.AskRequest;
import com.example.demo.model.AskResponse;
import com.example.demo.model.ReindexResponse;
import com.example.demo.service.RagService;

/**
 * REST API endpoints for RAG operations.
 * Simplified to use unified RagService.
 */
@RestController
@RequestMapping("/api/rag")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(value = "/reindex", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReindexResponse reindex(@RequestPart("file") MultipartFile file) throws Exception {
        long t0 = System.currentTimeMillis();
        int chunks = ragService.indexPdf(file);
        int vectors = ragService.getStoreSize();
        long dt = System.currentTimeMillis() - t0;
        return new ReindexResponse(chunks, vectors, dt);
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req) throws Exception {
        return ragService.ask(req.getQuestion());
    }

    @DeleteMapping("/clear")
    public void clearIndex() {
        ragService.clearIndex();
    }
}
