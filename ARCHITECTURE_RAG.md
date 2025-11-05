## Kiến trúc & Workflow RAG (Spring AI + OpenRouter + Hugging Face)

### 1) Tổng quan
- **Mục tiêu**: RAG (Retrieval-Augmented Generation) đọc PDF, tách đoạn, nhúng vector (Hugging Face), lưu vào `SimpleVectorStore`, truy vấn tương tự, build context và gọi model chat qua OpenRouter để trả lời.
- **Công nghệ chính**:
  - Spring Boot 3.5, Spring AI 1.0.0-M4
  - OpenRouter (OpenAI-compatible) cho Chat Completion
  - Hugging Face Inference API cho Embedding
  - Spring AI `PagePdfDocumentReader`, `TokenTextSplitter`, `SimpleVectorStore`

### 2) Luồng dữ liệu RAG end-to-end
1. Client upload PDF: `POST /api/rag/reindex` (multipart)
2. Service `RagService.indexPdf`:
   - Đọc PDF theo trang bằng `PagePdfDocumentReader`
   - Cắt đoạn bằng `TokenTextSplitter` (chunkSize/overlap cấu hình)
   - Gọi `vectorStore.add(chunks)` → `HuggingFaceEmbeddingModelAdapter` auto-embed qua HF API → lưu vector vào `SimpleVectorStore`
3. Khi hỏi: `POST /api/rag/ask` với `question`
   - `VectorStore.similaritySearch` auto-embed câu hỏi → lấy top-k chunks theo `min-score`
   - Ghép context từ chunks + build system prompt
   - Gọi model chat để tạo câu trả lời. Ứng dụng hỗ trợ hai cách:
     - Sử dụng Spring AI `ChatClient` (DSL) — ví dụ: `chatClient.prompt().system(...).user(...).options(...).call().content()` (khuyến nghị)
     - Hoặc gọi trực tiếp OpenRouter API qua `OpenRouterService` (REST) nếu cần điều khiển thấp hơn
4. Trả về `AskResponse` gồm `answer` + danh sách `sources` (chunkId, score)

### 3) Trách nhiệm từng file chính

#### Khởi động & cấu hình
- `src/main/java/com/example/demo/DemoApplication.java`
  - Load biến môi trường từ `.env` trước khi chạy Spring Boot
  - Loại trừ `OpenAiAutoConfiguration` (dùng cấu hình tuỳ biến)

- `src/main/java/com/example/demo/config/WebClientConfig.java`
  - Cấu hình `WebClient.Builder` với timeout mở rộng (120s), header OpenRouter (`HTTP-Referer`, `X-Title`)
  - Áp dụng toàn cục cho Spring AI (qua `OpenAiApi`)

- `src/main/java/com/example/demo/config/SpringAiConfig.java`
  - Tạo `ClientHttpRequestFactory` (timeout 120s)
  - Tạo `RestClient.Builder` (header OpenRouter) và inject vào `OpenAiApi`
  - Khởi tạo `OpenAiApi` với base-url + api-key OpenRouter
  - Tạo `OpenAiChatModel` và `ChatClient` (ví dụ: `ChatClient.builder(chatModel).build()`)
  - Sử dụng Prompt DSL của Spring AI để gọi model: `chatClient.prompt().system(...).user(...).options(...).call().content()`

- `src/main/java/com/example/demo/config/VectorStoreConfig.java`
  - Khởi tạo `SimpleVectorStore` với `HuggingFaceEmbeddingModelAdapter`
  - In-memory vector store thích hợp demo/dev

#### Lớp dịch vụ
- `src/main/java/com/example/demo/service/HuggingFaceEmbeddingService.java`
  - Gọi HF Inference API để tạo embedding (single/batch)
  - Batching, retry, và chuẩn hoá vector (L2) cho cosine similarity
  - Suy luận kích thước vector theo model

- `src/main/java/com/example/demo/service/HuggingFaceEmbeddingModelAdapter.java`
  - Adapter hiện thực `EmbeddingModel` của Spring AI bằng cách dùng `HuggingFaceEmbeddingService`
  - Cho phép `VectorStore` của Spring AI auto-embed `Document`/text

- `src/main/java/com/example/demo/service/OpenRouterService.java`
  - Gọi trực tiếp API `/v1/chat/completions` của OpenRouter bằng `WebClient`
  - Nhận `system` + `user` messages, `model`, `temperature`, `max_tokens` → trả `content`

- `src/main/java/com/example/demo/service/RagService.java`
  - Điểm tập trung workflow RAG
  - `indexPdf`:
    - `PagePdfDocumentReader` → `TokenTextSplitter` → `vectorStore.add`
    - Đếm số vectors (do `SimpleVectorStore` chưa hỗ trợ clear chính thức)
  - `ask`:
    - `vectorStore.similaritySearch` (top-k, min-score)
    - Build context + system prompt tiếng Việt (rule-based)
    - Gọi `OpenRouterService.chat` để tạo câu trả lời
  - `clearIndex`: chỉ reset counter (hạn chế của `SimpleVectorStore`)

#### API layer
- `src/main/java/com/example/demo/controller/ChatController.java`
  - `POST /api/rag/reindex` (multipart file) → `RagService.indexPdf`
  - `POST /api/rag/ask` (JSON) → `RagService.ask`
  - `DELETE /api/rag/clear` → `RagService.clearIndex`

#### Model DTOs
- `src/main/java/com/example/demo/model/AskRequest.java`: payload câu hỏi
- `src/main/java/com/example/demo/model/AskResponse.java`: câu trả lời + `sources`
- `src/main/java/com/example/demo/model/ReindexResponse.java`: số chunks/vectors và thời gian xử lý

### 4) Cấu hình & biến môi trường
- `src/main/resources/application.properties`
  - Server: `server.port=1234`
  - OpenRouter: `spring.ai.openai.api-key`, `spring.ai.openai.base-url`, `chat.options.model|temperature|max-tokens`
  - Hugging Face: `huggingface.api.key`, `huggingface.embedding.model`
  - RAG: `rag.chunk.size`, `rag.chunk.overlap`, `rag.retrieval.top-k`, `rag.retrieval.min-score`
  - Upload limit: 64MB

Gợi ý biến môi trường (qua `.env` hoặc hệ thống):
```
OPENROUTER_API_KEY=... 
OPENROUTER_BASE_URL=https://openrouter.ai/api
OPENROUTER_MODEL=meta-llama/llama-3.1-70b-instruct
OPENROUTER_TEMPERATURE=0.7
OPENROUTER_MAX_TOKENS=1000

HUGGINGFACE_API_KEY=...
HUGGINGFACE_EMBEDDING_MODEL=intfloat/multilingual-e5-large
```

### 5) Chuỗi xử lý chi tiết (sequence)
1. Reindex:
   - Controller nhận file → `RagService.indexPdf`
   - PDF → pages (`PagePdfDocumentReader`) → chunks (`TokenTextSplitter`)
   - `vectorStore.add(chunks)` → Adapter gọi HF tạo embeddings → lưu vector
2. Ask:
   - Controller nhận `question` → `RagService.ask`
   - `similaritySearch(question)` → top-k chunks (auto-embed question)
   - Build system prompt chứa context (tiếng Việt, rules hạn chế hallucination)
   - Gọi model chat để trả về content và `sources`.
     - Ưu tiên dùng Spring AI `ChatClient` (DSL): `chatClient.prompt().system(...).user(...).options(...).call().content()`
     - Hoặc gọi trực tiếp `OpenRouterService` (REST) khi cần kiểm soát thêm

### 6) Tham số quan trọng ảnh hưởng chất lượng
- **Chunking**: `rag.chunk.size` (mặc định 800), `rag.chunk.overlap` (100)
- **Retrieval**: `rag.retrieval.top-k` (5), `rag.retrieval.min-score` (0.35)
- **Model**: `spring.ai.openai.chat.options.model` (OpenRouter), `temperature`, `max-tokens`
- **Embedding model** (HF): ảnh hưởng trực tiếp tới chất lượng truy xuất; `multilingual-e5-large` gợi ý cho tiếng Việt

### 7) Hạn chế hiện tại & hướng mở rộng
- `SimpleVectorStore` là in-memory, không có `clear()` thực sự → cần restart app để xoá index. Khuyến nghị chuyển sang `PgVectorStore`, `Neo4jVectorStore` hoặc `PineconeVectorStore` khi production.
- Thiếu metadata nguồn (filename, page) trong `Document` để trích dẫn phong phú hơn → có thể set metadata khi đọc PDF.
- Prompt hiện inline trong `RagService` → có thể tách thành template ngoài + hỗ trợ RAG guardrails.
- Có thể thêm caching của embeddings/indices theo checksum file để tránh reindex toàn bộ.

### 8) Endpoints tóm tắt
- `POST /api/rag/reindex` (multipart form-data: `file`): reindex PDF, trả `{chunks, vectors, millis}`
- `POST /api/rag/ask` (JSON `{question}`): trả `{answer, sources:[{chunkId, score}]}`
- `DELETE /api/rag/clear`: reset counter (không xoá thực sự khỏi `SimpleVectorStore`)

### 9) Build & chạy
- Java 17, Maven
- Chạy: `mvn spring-boot:run` hoặc script `run.ps1`
- Đảm bảo set `OPENROUTER_API_KEY` và `HUGGINGFACE_API_KEY`


