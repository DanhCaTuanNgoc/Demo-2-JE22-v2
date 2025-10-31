package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunker {
    private final int size;
    private final int overlap;
    public TextChunker(@Value("${rag.chunk.size:1000}") int size,
                       @Value("${rag.chunk.overlap:200}") int overlap) {
        this.size = size; this.overlap = overlap;
    }
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null) return chunks;
        text = text.replaceAll("\r\n?", "\n");
        int n = text.length(), start = 0;
        while (start < n) {
            int end = Math.min(n, start + size);
            int bestEnd = end;
            for (int i = end; i > start + size/2 && i < n; i--) {
                if (i < n && (text.charAt(i-1) == '.' || text.charAt(i-1) == '\n')) { bestEnd = i; break; }
            }
            String chunk = text.substring(start, bestEnd).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            start = Math.max(bestEnd - overlap, start + size);
        }
        return chunks;
    }
}
