# RAG System với Spring AI + OpenRouter + Hugging Face

Hệ thống **Retrieval-Augmented Generation (RAG)** hoàn chỉnh sử dụng Spring AI framework để xây dựng chatbot trả lời câu hỏi dựa trên tài liệu PDF.

## 🎯 Tổng quan

Dự án này implement RAG pipeline end-to-end với các tính năng:
- 📄 **Upload PDF** và tự động index vào vector store
- 🔍 **Semantic search** với embeddings từ Hugging Face
- 💬 **Chat completion** qua OpenRouter (OpenAI-compatible API)
- 🎨 **Web UI** hiện đại với real-time source citations

## 🏗️ Kiến trúc

```
PDF Upload → PagePdfDocumentReader → TokenTextSplitter 
    ↓
HuggingFace Embedding → SimpleVectorStore (in-memory)
    ↓
User Question → Similarity Search → Build Context
    ↓
ChatClient (Spring AI) → OpenRouter API → Answer
```

### Công nghệ sử dụng

| Layer | Technology |
|-------|------------|
| **Backend Framework** | Spring Boot 3.5 |
| **AI Framework** | Spring AI 1.0.0-M4 |
| **Chat Model** | OpenRouter (Llama 3.1 70B) |
| **Embedding Model** | Hugging Face (multilingual-e5-large) |
| **Vector Store** | SimpleVectorStore (in-memory) |
| **PDF Processing** | Spring AI PagePdfDocumentReader |
| **Frontend** | HTML/CSS/JavaScript |

## 📋 Yêu cầu hệ thống

- **Java**: 17 trở lên
- **Maven**: 3.6+
- **API Keys**:
  - OpenRouter API key ([Get here](https://openrouter.ai/))
  - Hugging Face API token ([Get here](https://huggingface.co/settings/tokens))

## 🚀 Cài đặt & Chạy

### 1. Clone repository

```bash
git clone https://github.com/DanhCaTuanNgoc/Demo-2-JE22-v2.git
cd Demo-2-JE22-v2
```

### 2. Cấu hình API keys

Tạo file `.env` trong thư mục root:

```env
OPENROUTER_API_KEY=sk-or-v1-xxxxx
HUGGINGFACE_API_KEY=hf_xxxxx
```

Hoặc set trong `src/main/resources/application.properties`:

```properties
spring.ai.openai.api-key=${OPENROUTER_API_KEY}
huggingface.api.key=${HUGGINGFACE_API_KEY}
```

### 3. Build & Run

**Option 1: PowerShell script**
```powershell
.\run.ps1
```

**Option 2: Maven**
```bash
mvn spring-boot:run
```

### 4. Truy cập ứng dụng

Mở browser: **http://localhost:1234**

## 📖 Sử dụng

### Upload PDF và Index

```bash
curl -X POST http://localhost:1234/api/rag/reindex \
  -F "file=@document.pdf"
```

**Response:**
```json
{
  "chunks": 45,
  "vectors": 45,
  "processingTimeMs": 8234
}
```

### Đặt câu hỏi

```bash
curl -X POST http://localhost:1234/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Spring Framework là gì?"}'
```

**Response:**
```json
{
  "answer": "Spring Framework là framework Java phổ biến...",
  "sources": [
    {"chunkId": 0, "score": 0.873},
    {"chunkId": 1, "score": 0.652}
  ]
}
```

## 🔧 Cấu hình

### Chunking Strategy
```properties
rag.chunk.size=800
rag.chunk.overlap=100
```

### Retrieval Parameters
```properties
rag.retrieval.top-k=5
rag.retrieval.min-score=0.35
```

### Model Configuration
```properties
# Chat model
spring.ai.openai.chat.options.model=meta-llama/llama-3.1-70b-instruct
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=1000

# Embedding model
huggingface.embedding.model=intfloat/multilingual-e5-large
```

## 📁 Cấu trúc dự án

```
src/main/java/com/example/demo/
├── DemoApplication.java           # Entry point
├── config/
│   ├── SpringAiConfig.java        # ChatClient & OpenAiApi config
│   ├── VectorStoreConfig.java     # Vector store initialization
│   ├── WebClientConfig.java       # HTTP client timeout config
│   └── CorsConfig.java            # CORS settings
├── controller/
│   └── ChatController.java        # REST API endpoints
├── service/
│   ├── RagService.java            # Core RAG logic
│   ├── HuggingFaceEmbeddingService.java        # HTTP client for HF API
│   └── HuggingFaceEmbeddingModelAdapter.java   # Spring AI adapter
└── model/
    ├── AskRequest.java            # Request DTO
    ├── AskResponse.java           # Response DTO
    └── ReindexResponse.java       # Index response DTO

src/main/resources/
├── application.properties         # Configuration
└── static/
    └── index.html                 # Web UI
```

## 🎨 Kiến trúc chi tiết

### 1. PDF Indexing Pipeline

```java
// RagService.indexPdf()
PDF File
  → PagePdfDocumentReader.get()          // Load pages
  → TokenTextSplitter.apply()            // Split into chunks (800 chars)
  → vectorStore.add(chunks)              // Auto-embed & store
      → HuggingFaceEmbeddingModelAdapter // Spring AI bridge
          → HuggingFaceEmbeddingService  // HTTP client
              → POST /hf-inference/...   // Hugging Face API
              → L2 Normalization          // Cosine similarity prep
      → SimpleVectorStore                // In-memory storage
```

### 2. Question Answering Pipeline

```java
// RagService.ask()
User Question
  → vectorStore.similaritySearch()       // Auto-embed query
      → Cosine Similarity                // Compare with stored vectors
      → Top-K filtering (k=5)            // Get most relevant chunks
  → Build Context                        // Concatenate chunks
  → ChatClient.prompt()                  // Spring AI DSL
      .system(contextPrompt)             // Context + rules
      .user(question)                    // User question
      .options(temperature=0.7)          // Model config
      .call()                            // HTTP to OpenRouter
      .content()                         // Extract answer
  → Return answer + sources
```

### 3. Component Interaction

```
┌─────────────────────────────────────────────────────────┐
│ ChatController (REST API)                               │
└───────────────┬─────────────────────────────────────────┘
                │
                ↓
┌─────────────────────────────────────────────────────────┐
│ RagService (Business Logic)                             │
│   - indexPdf()                                          │
│   - ask()                                               │
└───────┬───────────────────────────┬─────────────────────┘
        │                           │
        ↓                           ↓
┌──────────────────┐    ┌──────────────────────────────┐
│ VectorStore      │    │ ChatClient                   │
│ (SimpleVector)   │    │ (Spring AI DSL)              │
└────┬─────────────┘    └────┬─────────────────────────┘
     │                       │
     ↓                       ↓
┌──────────────────┐    ┌──────────────────────────────┐
│ HuggingFace      │    │ OpenAiApi                    │
│ EmbeddingAdapter │    │ (OpenRouter client)          │
└────┬─────────────┘    └────┬─────────────────────────┘
     │                       │
     ↓                       ↓
┌──────────────────┐    ┌──────────────────────────────┐
│ HuggingFace      │    │ OpenRouter API               │
│ Service          │    │ (Llama 3.1 70B)              │
└──────────────────┘    └──────────────────────────────┘
```

## 🔑 Các pattern & best practices

### 1. Adapter Pattern
- `HuggingFaceEmbeddingModelAdapter`: Bridge giữa Spring AI interface và custom HuggingFace service
- Tách biệt framework code và business logic

### 2. Dependency Injection
- All components được Spring manage (testability)
- Configuration externalized (application.properties)

### 3. Timeout Strategy
- Connection timeout: 10s
- Read/write timeout: 120s (cho large PDFs)
- Retry logic: 3 attempts với exponential backoff

### 4. Embedding Optimization
- Batching: 10 texts/API call (thay vì 1)
- L2 Normalization: Đảm bảo cosine similarity chính xác
- Caching: Vectors được lưu in-memory (fast retrieval)

### 5. Prompt Engineering
```java
String systemPrompt = """
    Bạn là trợ lý AI phân tích tài liệu PDF.
    
    NHIỆM VỤ:
    Trả lời câu hỏi dựa HOÀN TOÀN trên context.
    
    QUY TẮC:
    1. CHỈ sử dụng thông tin có trong context
    2. Nếu không có đủ thông tin → nói rõ
    3. Trả lời bằng tiếng Việt
    4. KHÔNG bịa đặt thông tin
    
    === CONTEXT ===
    %s
    === END CONTEXT ===
    """.formatted(context);
```

## 📊 API Endpoints

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/api/rag/reindex` | Upload & index PDF | `multipart/form-data` (file) |
| POST | `/api/rag/ask` | Ask question | `{"question": "..."}` |
| DELETE | `/api/rag/clear` | Clear index | - |

## ⚡ Performance

### Typical Metrics
- **PDF Indexing**: ~8-12s cho 50-page PDF
  - Page loading: ~1s
  - Chunking: ~0.5s
  - Embedding: ~6-10s (batched API calls)
  - Storage: ~0.5s

- **Question Answering**: ~2-4s
  - Query embedding: ~0.5-1s
  - Similarity search: ~0.1s (in-memory)
  - LLM generation: ~1-3s

### Optimization Tips
1. **Increase batch size** (cẩn thận với API limits):
   ```java
   private static final int BATCH_SIZE = 20; // default: 10
   ```

2. **Reduce chunk overlap**:
   ```properties
   rag.chunk.overlap=50  # default: 100
   ```

3. **Use smaller embedding model** (trade-off: accuracy):
   ```properties
   huggingface.embedding.model=sentence-transformers/all-MiniLM-L6-v2
   ```

## 🚨 Hạn chế hiện tại

1. **In-memory Vector Store**
   - Mất data khi restart app
   - Không scale cho multi-instance
   - **Solution**: Migrate to PgVectorStore hoặc Pinecone

2. **No persistent storage**
   - Phải reindex PDF mỗi lần restart
   - **Solution**: Save vectors to database

3. **Single-file upload**
   - Chỉ có thể upload 1 PDF tại một thời điểm
   - **Solution**: Batch upload API

4. **No metadata tracking**
   - Thiếu thông tin về source file, page number
   - **Solution**: Enhance Document metadata

## 🔮 Hướng phát triển

- [ ] Migrate to **PgVectorStore** (persistent storage)
- [ ] Add **file metadata** (filename, upload date, page numbers)
- [ ] Implement **incremental indexing** (không cần reindex toàn bộ)
- [ ] Add **streaming response** cho chat
- [ ] Implement **re-ranking** với cross-encoder
- [ ] Add **authentication & authorization**
- [ ] Support **multiple file formats** (DOCX, TXT, etc.)
- [ ] Implement **conversation history**

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👨‍💻 Author

**DanhCaTuanNgoc**
- GitHub: [@DanhCaTuanNgoc](https://github.com/DanhCaTuanNgoc)

## 🙏 Acknowledgments

- [Spring AI](https://docs.spring.io/spring-ai/reference/) - AI framework
- [OpenRouter](https://openrouter.ai/) - LLM API gateway
- [Hugging Face](https://huggingface.co/) - Embedding models
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework

## 📚 Tài liệu bổ sung

- [ARCHITECTURE_RAG.md](./ARCHITECTURE_RAG.md) - Kiến trúc chi tiết
- [Spring AI Docs](https://docs.spring.io/spring-ai/reference/)
- [OpenRouter Models](https://openrouter.ai/models)
- [Hugging Face Models](https://huggingface.co/models?pipeline_tag=sentence-similarity)

---

**⭐ If you find this project helpful, please give it a star!**
