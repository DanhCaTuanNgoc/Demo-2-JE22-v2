package com.example.demo.service;

public class VectorMath {
    public static double dot(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double s = 0;
        for (int i = 0; i < n; i++) s += a[i] * b[i];
        return s;
    }
    public static double norm(float[] a) {
        double s = 0;
        for (float v : a) s += v * v;
        return Math.sqrt(s);
    }
    public static double cosine(float[] a, float[] b) {
        double na = norm(a), nb = norm(b);
        if (na == 0 || nb == 0) return 0.0;
        return dot(a, b) / (na * nb);
    }
}
