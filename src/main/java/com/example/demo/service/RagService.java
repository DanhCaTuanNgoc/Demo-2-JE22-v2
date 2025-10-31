package com.example.demo.service;

import com.example.demo.model.AskResponse;
import com.example.demo.service.IntentDetector.Hint;
import com.example.demo.service.IntentDetector.Intent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Value("${rag.top.k:4}")
    private int topK;

    @Value("${rag.threshold:0.35}")
    private double minScore;

    private final VectorStore store;
    private final AIClient aiClient;

    public RagService(VectorStore store, AIClient aiClient) {
        this.store = store;
        this.aiClient = aiClient;
    }

    public AskResponse ask(String question) throws Exception {
        if (store.size() == 0) {
            return new AskResponse(
                "Chưa upload dữ liệu PDF.",
                List.of()
            );
        }

        Hint hint = IntentDetector.detect(question);

    float[] q = aiClient.embed(question);
        int oversample = Math.max(topK * 3, topK + 2);
        List<VectorStore.Scored> firstPass = store.topK(q, oversample);

        if (firstPass.isEmpty() || firstPass.get(0).score() < minScore) {
            return new AskResponse("Tôi không biết.", List.of());
        }

        List<VectorStore.Scored> rescored = new ArrayList<>();
        String sec = hint.sectionHint == null ? null : hint.sectionHint.toLowerCase(Locale.ROOT);

        for (var s : firstPass) {
            double boost = 0.0;
            String textLower = s.text().toLowerCase(Locale.ROOT);

            if (sec != null) {
                if (textLower.contains(sec)) boost += 0.08;
                if (sec.contains("mở đầu") &&
                    (textLower.contains("mở đầu") || textLower.contains("introduction"))) {
                    boost += 0.06;
                }
            }
            if (hint.intent == Intent.DEFINE && hint.term != null &&
                textLower.contains(hint.term.toLowerCase(Locale.ROOT))) {
                boost += 0.12;
            }
            rescored.add(new VectorStore.Scored(s.id(), s.text(), s.score() + boost));
        }
        rescored.sort(Comparator.comparingDouble(VectorStore.Scored::score).reversed());

        if (rescored.isEmpty() || rescored.get(0).score() < minScore) {
            return new AskResponse("Tôi không biết.", List.of());
        }

        var hits = rescored.subList(0, Math.min(topK, rescored.size()));

        StringBuilder ctx = new StringBuilder();
        for (VectorStore.Scored s : hits) {
            ctx.append("\n[Chunk #").append(s.id())
               .append(" / score=").append(String.format("%.3f", s.score()))
               .append("]\n").append(s.text()).append("\n");
        }

        String system = switch (hint.intent) {
            case SUMMARY -> """
                    Bạn là trợ lý AI chuyên phân tích tài liệu. Nhiệm vụ: tóm tắt nội dung.
                    
                    QUY TẮC:
                    - CHỈ sử dụng thông tin từ context được cung cấp
                    - Trả lời ngắn gọn, súc tích bằng tiếng Việt
                    - Tập trung vào ý chính, không thêm thông tin ngoài context
                    - Nếu context không đủ thông tin, hãy nói rõ "Tôi không có đủ thông tin để trả lời"
                    
                    ĐỊNH DẠNG: Đoạn văn ngắn (3-5 câu)
                    """;
            case BULLET_SUMMARY -> """
                    Bạn là trợ lý AI chuyên phân tích tài liệu. Nhiệm vụ: tóm tắt dạng bullet points.
                    
                    QUY TẮC:
                    - CHỈ sử dụng thông tin từ context được cung cấp
                    - Trả lời bằng danh sách các điểm chính (bullet points) bằng tiếng Việt
                    - Mỗi bullet point là một câu hoàn chỉnh, rõ ràng
                    - Không thêm thông tin không có trong context
                    - Nếu context không đủ, hãy nói rõ "Tôi không có đủ thông tin"
                    
                    ĐỊNH DẠNG: Danh sách có dấu đầu dòng, mỗi điểm một câu
                    """;
            case DEFINE -> """
                    Bạn là trợ lý AI chuyên phân tích tài liệu. Nhiệm vụ: định nghĩa khái niệm.
                    
                    QUY TẮC:
                    - CHỈ sử dụng định nghĩa từ context được cung cấp
                    - Trả lời chính xác, ngắn gọn bằng tiếng Việt
                    - Nếu có định nghĩa rõ ràng trong context, trích dẫn ngắn (1-2 câu)
                    - Nếu không tìm thấy định nghĩa chính xác, nói rõ "Tôi không tìm thấy định nghĩa trong tài liệu"
                    - Không suy luận hay thêm ý kiến cá nhân
                    
                    ĐỊNH DẠNG: Định nghĩa ngắn gọn, rõ ràng
                    """;
            case COMPARE -> """
                    Bạn là trợ lý AI chuyên phân tích tài liệu. Nhiệm vụ: so sánh các khái niệm.
                    
                    QUY TẮC:
                    - CHỈ sử dụng thông tin từ context được cung cấp
                    - So sánh theo cấu trúc rõ ràng bằng tiếng Việt:
                      • Điểm mạnh/Khi nào dùng A
                      • Điểm mạnh/Khi nào dùng B
                    - Nếu thiếu thông tin về bất kỳ phương án nào, nói rõ ràng
                    - Không thêm ý kiến chủ quan
                    
                    ĐỊNH DẠNG: So sánh có cấu trúc, dễ đọc
                    """;
            default -> """
                    Bạn là trợ lý AI chuyên phân tích tài liệu PDF.
                    
                    QUY TẮC:
                    - CHỈ trả lời dựa trên context được cung cấp
                    - Trả lời ngắn gọn, rõ ràng bằng tiếng Việt
                    - Nếu context không chứa thông tin cần thiết, nói rõ "Tôi không tìm thấy thông tin này trong tài liệu"
                    - Không bịa đặt hay suy luận ngoài context
                    - Luôn thân thiện và chuyên nghiệp
                    
                    ĐỊNH DẠNG: Câu trả lời súc tích, dễ hiểu
                    """;
        };

        String formatPart = switch (hint.intent) {
            case BULLET_SUMMARY ->
                (hint.bullets != null ? "Return exactly " + hint.bullets + " bullets.\n" : "Return 3-6 bullets.\n");
            case DEFINE -> (hint.term != null ? "Term to define: \"" + hint.term + "\"\n" : "");
            case COMPARE -> "If the question mentions two methods A and B, structure bullets per method.\n";
            default -> "";
        };

        String userPrompt = "=== CONTEXT START ===\n" + ctx + "=== CONTEXT END ===\n\n"
                + formatPart + "Question: " + question + "\nAnswer:";

        String answer = aiClient.chatWithSystem(system, userPrompt);

        return new AskResponse(
            answer,
            hits.stream()
                .map(s -> new AskResponse.SourceScore(s.id(), s.score()))
                .collect(Collectors.toList())
        );
    }
}
