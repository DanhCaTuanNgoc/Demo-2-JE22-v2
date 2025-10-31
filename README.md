Demo 2 — AI Chat RAG từ PDF (Spring Boot + OpenRouter / Spring AI)

Ứng dụng cung cấp hỏi đáp theo nội dung PDF.

Quy trình RAG: Nạp PDF → Tách đoạn (chunk) → Tạo vector embedding (Gemini) → Truy xuất top-k ngữ cảnh.

✨ Tính năng

- ✅ **Upload & Reindex PDF**: Chọn file là tự trích văn bản → chunk → embedding.
- ✅ **RAG**: Truy xuất **top-K** đoạn liên quan, dựng **context** và **prompt**.
- ✅ **UI**: Nút "+" nạp PDF, bong bóng AI trái – Người dùng phải.
- ✅ **Web Interface**: Giao diện web đơn giản và thân thiện
- ✅ **REST API gọn**: API endpoints để tương tác với chatbot
- ✅ **UTF-8 Support**: Hỗ trợ đầy đủ tiếng Việt
- ✅ **Error Handling**: Xử lý lỗi thân thiện và chi tiết




📦 Tech stack

- Java 17, Spring Boot 3 (Web, Validation, Actuator)
- Apache PDFBox
- Gemini APIs (chat & embedding)
- Frontend: HTML/CSS/JS nguyên bản

⚙️ Cấu hình & chạy

### 1. Cấu hình OpenRouter (OpenAI-compatible)

Bạn cần có OpenRouter API Key (https://openrouter.ai/keys) hoặc một endpoint OpenAI-compatible.

1. Truy cập https://openrouter.ai/keys và tạo API key.
2. Hoặc dùng biến môi trường OpenRouter cung cấp.

Thêm vào `application.properties` hoặc biến môi trường (ví dụ PowerShell):

```powershell
$env:OPENROUTER_API_KEY="your_openrouter_api_key_here"
```

Hoặc trực tiếp trong `application.properties` (không khuyến nghị commit key):

```properties
spring.ai.openai.api-key=your_openrouter_api_key_here
spring.ai.openai.base-url=https://api.openrouter.ai/v1
spring.ai.openai.model=gpt-4o-mini
spring.ai.openai.embeddings-model=text-embedding-3-small
```

**Cách 2: Set environment variables trực tiếp**

```bash
# Windows (PowerShell)
$env:GEMINI_API_KEY="your_actual_gemini_api_key_here"

# Linux/Mac
export GEMINI_API_KEY="your_actual_gemini_api_key_here"
```

**Cách 3: Cập nhật trực tiếp trong application.properties**

```properties
gemini.api.key=your_actual_gemini_api_key_here
gemini.api.url=https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent
```
      cp config-template.properties .env

> ⚠️ **Lưu ý bảo mật**: Không commit API key vào Git. File `.env` đã được thêm vào `.gitignore`.

### 3. Chạy ứng dụng

```bash
# Chạy với Maven (Windows)
mvn spring-boot:run

# Chạy với Maven (Linux/Mac)
./mvnw spring-boot:run

# Hoặc build và chạy
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### 4. Truy cập ứng dụng

Mở trình duyệt và truy cập: **http://localhost:8080**

## 🧪 Test Demo

### Test Upload & Reindex (nạp PDF)

```
Bấm "+" chọn file PDF → Đang nạp file.

Khi xong, trạng thái hiển thị: Đã nạp: <tên file>

```

### Test Hỏi – Đáp dựa vào PDF (RAG)

```
User: "Tóm tắt ngắn tài liệu này."
Bot: "…(Tóm tắt)…"

User: "Nêu 5 ý quan trọng nhất của tài liệu."
Bot: 
1) …
2) …
3) …
4) …
5) …

User: "Liệt kê 3 thuật ngữ/cụm từ khóa quan trọng và giải thích ngắn gọn."
Bot:
- **Thuật ngữ A**: …
- **Thuật ngữ B**: …
- **Thuật ngữ C**: …

```

📁 Cấu trúc dự án

```
demo-2/
├── src/main/java/com/example/demo/
│   ├── config/
│   │   └── CorsConfig.java               # Cấu hình CORS
│   ├── controller/
│   │   ├── ChatController.java           # REST API endpoints
│   │   └── GlobalExceptionHandler.java   # Bắt lỗi chung, trả JSON gọn cho UI
│   ├── model/
│   │   ├── AskRequest.java               # question
│   │   ├── AskResponse.java              # answer, sources[]
│   │   └── ReindexResponse.java          # chunks, vectors, millis
│   ├── service/
│   │   ├── DemoApplication.java          # Main Spring Boot application
│   │   ├── GeminiRestService.java        # Gọi Gemini: chat + embedding
│   │   ├── IndexService.java             # Upload & Reindex PDF → vectors
│   │   ├── IntentDetector.java           # Nhận diện intent
│   │   ├── PdfLoader.java                # Trích văn bản từ PDF
│   │   ├── RagService.java               # Truy xuất PDF, dùng Gemini để trả lời
│   │   ├── TextChunker.java              # Chia chunk + overlap
│   │   ├── VectorMath.java               # cosine, dot, norm
│   │   └── VectorStore.java              # Lưu vector in-memory, topK()
│
├── src/main/resources/
│   ├── static/
│   │   └── index.html                    # Web interface
│   └── application.properties            # Cấu hình ứng dụng
│
├── config-template.properties            # Mẫu để tạo .env
├── .gitignore                            # Git ignore file
├── pom.xml                               # Maven dependencies & plugins
└── README.md                             # Tài liệu dự án

```
## 📦Dependencies chính

- **Spring Boot 3.5.6**: Framework chính
- **Spring Web**: REST API và web interface
- **Java 17**: Runtime environment
- **Maven**: Build tool và dependency management