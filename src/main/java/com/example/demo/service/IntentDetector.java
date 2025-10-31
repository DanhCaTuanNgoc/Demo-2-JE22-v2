package com.example.demo.service;

import java.util.Locale;
import java.util.regex.Pattern;

public class IntentDetector {

    public enum Intent {
        SUMMARY, BULLET_SUMMARY, DEFINE, COMPARE, GENERIC
    }

    public static class Hint {
        public final Intent intent;
        public final Integer bullets;
        public final String term;
        public final String sectionHint;

        public Hint(Intent i, Integer b, String t, String s) {
            intent = i;
            bullets = b;
            term = t;
            sectionHint = s;
        }
    }

    public static Hint detect(String q) {
        String s = q.toLowerCase(Locale.ROOT).trim();

        String section = null;
        var pChapter = Pattern.compile("(chương\\s*\\d+|chapter\\s*\\d+|section\\s*\\d+(?:\\.\\d+)*)");
        var mChapter = pChapter.matcher(s);
        if (mChapter.find())
            section = mChapter.group();
        if (s.contains("mở đầu") || s.contains("introduction"))
            section = (section == null ? "mở đầu" : section);

        Integer bullets = null;
        var pBullets = Pattern.compile("(\\d+)\\s*(gạch đầu dòng|bullet|ý chính)");
        var mBullets = pBullets.matcher(s);
        if (mBullets.find()) {
            try {
                bullets = Integer.parseInt(mBullets.group(1));
            } catch (Exception ignore) {
            }
        }

        if (s.startsWith("tài liệu này nói về") || s.startsWith("tài liệu này nói về gì")
                || (s.contains("tóm tắt") && bullets == null))
            return new Hint(Intent.SUMMARY, null, null, section);

        if (s.contains("tóm tắt") && bullets != null)
            return new Hint(Intent.BULLET_SUMMARY, bullets, null, section);

        if (s.startsWith("định nghĩa") || s.contains("define ") || s.startsWith("what is "))
            return new Hint(Intent.DEFINE, null, extractQuoted(q), section);

        if (s.startsWith("so sánh") || s.contains("compare"))
            return new Hint(Intent.COMPARE, null, null, section);

        return new Hint(Intent.GENERIC, null, null, section);
    }

    private static String extractQuoted(String s) {
        var m1 = Pattern.compile("[‘'\\\"]([^‘'\\\"]{2,})[’'\\\"]").matcher(s);
        if (m1.find())
            return m1.group(1);
        return null;
    }
}
