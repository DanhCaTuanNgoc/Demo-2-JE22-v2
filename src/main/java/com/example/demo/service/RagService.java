package com.example.demo.service;

import com.example.demo.model.AskResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Unified RAG service using pure Spring AI framework.
 * 
 * Pipeline:
 * 1. PDF → PagePdfDocumentReader (Spring AI)
 * 2. Documents → TokenTextSplitter (Spring AI)
 * 3. Chunks → HuggingFaceEmbeddingModelAdapter → SimpleVectorStore (Spring AI)
 * 4. Query → VectorStore.similaritySearch (auto-embed via adapter)
 * 5. Context → ChatClient.prompt().call() (Pure Spring AI)
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private TokenTextSplitter textSplitter;
    
    // Track total vectors stored
    private int totalVectors = 0;

    @Value("${spring.ai.openai.chat.options.model:meta-llama/llama-3.1-70b-instruct}")
    private String chatModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    @Value("${rag.retrieval.top-k:5}")
    private int topK;

    @Value("${rag.retrieval.min-score:0.35}")
    private double minScore;

    @Value("${rag.chunk.size:800}")
    private int chunkSize;

    @Value("${rag.chunk.overlap:100}")
    private int chunkOverlap;

    public RagService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }
    
    @PostConstruct
    public void init() {
        // Initialize TokenTextSplitter after @Value properties are injected
        // Parameters: defaultChunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator
        this.textSplitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        
        System.out.println("✅ RagService initialized with Spring AI components");
        System.out.println("   📄 PDF Reader: PagePdfDocumentReader");
        System.out.println("   ✂️  Text Splitter: TokenTextSplitter (" + chunkSize + " chars, " + chunkOverlap + " overlap)");
        System.out.println("   🗄️  Vector Store: SimpleVectorStore");
        System.out.println("   🤖 Chat Model: " + chatModel);
    }

    /**
     * Index a PDF document into vector store using Spring AI pipeline.
     * 
     * @param file PDF file to index
     * @return number of chunks indexed
     */
    public int indexPdf(MultipartFile file) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📄 Starting Spring AI PDF indexing: " + file.getOriginalFilename());
        System.out.println("=".repeat(80));
        
        // 1. Load PDF using Spring AI PagePdfDocumentReader
        System.out.println("📖 Step 1: Loading PDF with PagePdfDocumentReader...");
        
        // Convert MultipartFile to Resource
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
        List<Document> documents = pdfReader.get();
        
        System.out.println("   ✅ Loaded " + documents.size() + " pages from PDF");

        if (documents.isEmpty()) {
            throw new Exception("No pages extracted from PDF");
        }

        // 2. Split documents into chunks using TokenTextSplitter
        System.out.println("✂️  Step 2: Splitting into chunks with TokenTextSplitter...");
        
        List<Document> chunks = textSplitter.apply(documents);
        
        System.out.println("   ✅ Created " + chunks.size() + " chunks");

        if (chunks.isEmpty()) {
            throw new Exception("No chunks created from documents");
        }

        // Log sample chunk
        if (!chunks.isEmpty()) {
            Document firstChunk = chunks.get(0);
            System.out.println("   📝 Sample chunk #0:");
            System.out.println("      Length: " + firstChunk.getContent().length() + " chars");
            System.out.println("      Preview: " + firstChunk.getContent().substring(0, Math.min(100, firstChunk.getContent().length())) + "...");
        }

        // 3. Add chunks to VectorStore (auto-embed via HuggingFaceEmbeddingModelAdapter)
        System.out.println("🔄 Step 3: Adding chunks to SimpleVectorStore (auto-embedding via HuggingFace)...");
        
        vectorStore.add(chunks);
        totalVectors += chunks.size();
        
        System.out.println("   ✅ Indexed " + chunks.size() + " chunks successfully");
        System.out.println("   📊 Total vectors in store: " + totalVectors);
        System.out.println("=".repeat(80));
        System.out.println("✅ PDF indexing complete!\n");

        return chunks.size();
    }

    /**
     * Ask a question using Spring AI RAG pipeline.
     * 
     * @param question User question
     * @return Answer with source chunks
     */
    public AskResponse ask(String question) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("� New Question: " + question);
        System.out.println("=".repeat(80));


        // ==========================================================
        // 1. Retrieve relevant documents using Spring AI VectorStore
        System.out.println("🔍 Step 1: Similarity search with VectorStore (auto-embed query)...");
        
        SearchRequest searchRequest = SearchRequest.query(question)
                .withTopK(topK)
                .withSimilarityThreshold(minScore);
        
        List<Document> results = vectorStore.similaritySearch(searchRequest);
        
        System.out.println("   ✅ Found " + results.size() + " relevant documents");

        if (results.isEmpty()) {
            System.out.println("   ⚠️  No results above similarity threshold (" + minScore + ")");
            return new AskResponse(
                    "Không tìm thấy thông tin liên quan trong tài liệu để trả lời câu hỏi này.",
                    new ArrayList<>()
            );
        }

        // Log top result similarity score
        if (!results.isEmpty()) {
            Document topDoc = results.get(0);
            // Spring AI stores similarity score in metadata
            Object scoreObj = topDoc.getMetadata().get("distance");
            if (scoreObj == null) {
                scoreObj = topDoc.getMetadata().get("score");
            }
            
            if (scoreObj != null) {
                double score = scoreObj instanceof Double ? (Double) scoreObj : 
                              scoreObj instanceof Float ? ((Float) scoreObj).doubleValue() : 0.0;
                System.out.println("   🏆 Top result similarity: " + String.format("%.3f", score));
            }
        }

        // =========================================
        // 2. Build context from retrieved documents
        System.out.println("📝 Step 2: Building context from retrieved chunks...");
        
        StringBuilder contextBuilder = new StringBuilder();
        List<AskResponse.SourceScore> sources = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            
            // Extract similarity score from metadata
            Object scoreObj = doc.getMetadata().get("distance");
            if (scoreObj == null) {
                scoreObj = doc.getMetadata().get("score");
            }
            
            double score = scoreObj instanceof Double ? (Double) scoreObj : 
                          scoreObj instanceof Float ? ((Float) scoreObj).doubleValue() : 0.0;

            contextBuilder.append("\n[Chunk #").append(i)
                    .append(" / similarity=").append(String.format("%.3f", score))
                    .append("]\n")
                    .append(doc.getContent())
                    .append("\n");
            
            sources.add(new AskResponse.SourceScore(i, score));
        }

        String context = contextBuilder.toString();
        System.out.println("   ✅ Context built with " + results.size() + " chunks");
        System.out.println("   � Total context length: " + context.length() + " characters");


        // ==========================================================   
        // 3. Build system prompt with context (Spring AI PromptBuilder pattern)
        System.out.println("🤖 Step 3: Building prompt with ChatClient DSL...");
        
        String systemPrompt = """
                Bạn là trợ lý AI phân tích tài liệu PDF thông minh.
                
                NHIỆM VỤ:
                Trả lời câu hỏi dựa HOÀN TOÀN trên context được cung cấp bên dưới.
                
                QUY TẮC BẮT BUỘC:
                1. CHỈ sử dụng thông tin có trong context
                2. Nếu không có đủ thông tin → trả lời: "Tôi không tìm thấy thông tin để trả lời câu hỏi này trong tài liệu"
                3. Trả lời bằng tiếng Việt, rõ ràng và chính xác
                4. Trích dẫn trực tiếp từ context nếu có thể
                5. KHÔNG bịa đặt hoặc suy luận ngoài context
                
                === CONTEXT START ===
                %s
                === CONTEXT END ===
                
                Bây giờ hãy trả lời câu hỏi của người dùng.
                """.formatted(context);

        // =====================================================
        // 4. Generate answer using Spring AI ChatClient - PURE FRAMEWORK
        System.out.println("💬 Step 4: Generating answer with Spring AI ChatClient...");
        System.out.println("   🤖 Model: " + chatModel);
        System.out.println("   🌡️  Temperature: " + temperature);
        System.out.println("   📊 Max Tokens: " + maxTokens);
        
        try {
            // DÙNG THUẦN SPRING AI CHATCLIENT
            String answer = chatClient.prompt()
                    .system(systemPrompt)       // System prompt với context từ PDF
                    .user(question)             // Câu hỏi của user
                    .options(OpenAiChatOptions.builder()
                            .withModel(chatModel)
                            .withTemperature(temperature)
                            .withMaxTokens(maxTokens)
                            .build())
                    .call()                     // Spring AI handles HTTP internally
                    .content();                 // Extract response content

            System.out.println("   ✅ Answer generated successfully");
            System.out.println("   📝 Answer length: " + (answer != null ? answer.length() : 0) + " characters");
            System.out.println("=".repeat(80) + "\n");

            return new AskResponse(
                    answer != null ? answer : "Không thể tạo câu trả lời.",
                    sources
            );
            
        } catch (Exception e) {
            System.err.println("   ❌ Error generating answer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate answer: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all indexed data from SimpleVectorStore.
     * Note: SimpleVectorStore doesn't expose clear/delete methods publicly.
     * This is a limitation of Spring AI SimpleVectorStore.
     */
    public void clearIndex() {
        totalVectors = 0;
        System.out.println("⚠️ SimpleVectorStore doesn't support clear operation");
        System.out.println("   Counter reset. Please restart application to clear index");
    }

    /**
     * Get current store size.
     */
    public int getStoreSize() {
        return totalVectors;
    }
}
