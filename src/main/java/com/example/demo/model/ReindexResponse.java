package com.example.demo.model;

public class ReindexResponse {
    private int chunks;
    private int vectors;
    private long millis;
    public ReindexResponse(int chunks, int vectors, long millis) {
        this.chunks = chunks; this.vectors = vectors; this.millis = millis;
    }
    public int getChunks() { return chunks; }
    public int getVectors() { return vectors; }
    public long getMillis() { return millis; }
}
