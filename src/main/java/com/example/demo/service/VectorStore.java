package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VectorStore {
    public static record Entry(int id, String text, float[] vec) {}
    public static record Scored(int id, String text, double score) {}

    private final List<Entry> entries = new ArrayList<>();

    public void clear() { entries.clear(); }
    public void add(int id, String text, float[] vec) { entries.add(new Entry(id, text, vec)); }
    public int size() { return entries.size(); }

    public List<Scored> topK(float[] query, int k) {
        List<Scored> scores = new ArrayList<>();
        for (Entry e : entries) {
            double s = VectorMath.cosine(query, e.vec());
            scores.add(new Scored(e.id(), e.text(), s));
        }
        scores.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scores.subList(0, Math.min(k, scores.size()));
    }
}
