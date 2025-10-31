package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class IndexService {
    private final VectorStore store;
    private final AIClient aiClient;
    private final PdfLoader pdf;
    private final TextChunker chunker;

    public IndexService(VectorStore store, AIClient aiClient, PdfLoader pdf, TextChunker chunker) {
        this.store = store; this.aiClient = aiClient; this.pdf = pdf; this.chunker = chunker;
    }

    public int reindexPdf(MultipartFile file) throws Exception {
        store.clear();
        String text = pdf.extractText(file.getInputStream());
        List<String> chunks = chunker.split(text);
        for (int i = 0; i < chunks.size(); i++) {
            float[] emb = aiClient.embed(chunks.get(i));
            store.add(i, chunks.get(i), emb);
        }
        return chunks.size();
    }
    public int getStoreSize() { return store.size(); }
}
