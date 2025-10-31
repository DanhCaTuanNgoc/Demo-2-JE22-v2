package com.example.demo.model;

import java.util.List;

public class AskResponse {
    private String answer;
    private List<SourceScore> sources;

    public AskResponse(String answer, List<SourceScore> sources) {
        this.answer = answer;
        this.sources = sources;
    }
    public String getAnswer() { return answer; }
    public List<SourceScore> getSources() { return sources; }

    public static class SourceScore {
        private int chunkId;
        private double score;
        public SourceScore(int chunkId, double score) {
            this.chunkId = chunkId;
            this.score = score;
        }
        public int getChunkId() { return chunkId; }
        public double getScore() { return score; }
    }
}
