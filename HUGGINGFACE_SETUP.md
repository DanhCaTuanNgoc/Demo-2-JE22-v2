# 🤗 Hướng dẫn sử dụng Hugging Face Embedding

## ✅ Đã cấu hình xong!

Hệ thống RAG của bạn đã được cấu hình với:
- **Chat**: OpenRouter (Llama 3.1 70B - MIỄN PHÍ)
- **Embedding**: Hugging Face (sentence-transformers/all-MiniLM-L6-v2 - MIỄN PHÍ)

---

## 📋 Bước tiếp theo:

### 1. **Lấy Hugging Face API Token**

1. Đăng ký tài khoản (nếu chưa có): https://huggingface.co/join
2. Truy cập: https://huggingface.co/settings/tokens
3. Click **"New token"**
4. Đặt tên: `rag-demo-token`
5. Chọn role: **Read**
6. Click **"Generate token"**
7. **Copy token**

### 2. **Cập nhật file `.env`**

Mở file `.env` và thay thế:

```properties
HUGGINGFACE_API_KEY=your_huggingface_token_here
```

Bằng token vừa lấy được:

```properties
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3. **Set biến môi trường (PowerShell)**

```powershell
$env:OPENROUTER_API_KEY="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"
$env:OPENROUTER_BASE_URL="https://openrouter.ai/api/v1"
$env:OPENROUTER_MODEL="meta-llama/llama-3.1-70b-instruct"
$env:HUGGINGFACE_API_KEY="your_huggingface_token_here"
```

### 4. **Chạy ứng dụng**

```powershell
mvn spring-boot:run
```

---

## 🧪 Test Hugging Face Embedding

### Test qua curl (sau khi lấy token):

```powershell
$headers = @{
    "Authorization" = "Bearer your_huggingface_token_here"
    "Content-Type" = "application/json"
}

$body = @{
    "inputs" = "Spring AI là gì?"
    "options" = @{
        "wait_for_model" = $true
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2" -Method Post -Headers $headers -Body $body
```

Kết quả: Mảng 384 số (vector embedding)

---

## 📊 Thông tin Embedding Model:

- **Model**: `sentence-transformers/all-MiniLM-L6-v2`
- **Dimensions**: 384
- **Max sequence length**: 256 tokens
- **Tốc độ**: ~50ms/request
- **Giới hạn**: 1000 requests/giờ (free tier)

---

## 🔥 Workflow hoàn chỉnh:

```
1. Upload PDF
    ↓
2. PdfLoader extract text
    ↓
3. TextChunker chia thành chunks
    ↓
4. 🤗 Hugging Face API: chunks → vectors (384 dims)
    ↓
5. VectorStore lưu trữ vectors
    ↓
6. User hỏi câu hỏi
    ↓
7. 🤗 Hugging Face: question → vector
    ↓
8. VectorStore tìm kiếm (cosine similarity)
    ↓
9. Lấy top-K chunks
    ↓
10. 🤖 OpenRouter (Llama 3.1): Context + Question → Answer
```

---

## ⚠️ Lưu ý:

### **Cold start (lần đầu):**
- Hugging Face model cần ~5-10 giây để khởi động lần đầu
- Các request tiếp theo sẽ nhanh hơn (~50-100ms)

### **Rate limits:**
- **Free tier**: 1000 requests/giờ
- **Pro**: Unlimited

### **Nếu gặp lỗi 503 (Model Loading):**
```
{
  "inputs": "test",
  "options": {
    "wait_for_model": true  ← Quan trọng!
  }
}
```

---

## 🎯 So sánh với các giải pháp khác:

| | Hugging Face | Gemini | Ollama |
|---|--------------|--------|--------|
| **Cài đặt** | ✅ Chỉ API key | ✅ Chỉ API key | ❌ Phải tải model |
| **Tốc độ** | ⚡ 50-100ms | ⚡ 100-200ms | ⚡⚡ 10-30ms |
| **Chi phí** | ✅ Miễn phí | ✅ Miễn phí | ✅ Miễn phí |
| **Offline** | ❌ | ❌ | ✅ |
| **Dimensions** | 384 | 768 | 768 |
| **Giới hạn** | 1000/giờ | 60/phút | ∞ |

---

## 🚀 Bước tiếp theo:

1. ✅ Lấy Hugging Face token
2. ✅ Cập nhật `.env`
3. ✅ Chạy `mvn spring-boot:run`
4. ✅ Truy cập http://localhost:1234
5. ✅ Upload PDF và test!

---

## 📝 Files đã được cập nhật:

- ✅ `.env` - Thêm Hugging Face config
- ✅ `application.properties` - Disable Spring AI embedding
- ✅ `HuggingFaceEmbeddingService.java` - NEW: Custom embedding service
- ✅ `SpringAiService.java` - Sử dụng Hugging Face cho embedding
- ✅ `SpringAiConfig.java` - Chỉ config chat, không config embedding

Chúc bạn thành công! 🎉
