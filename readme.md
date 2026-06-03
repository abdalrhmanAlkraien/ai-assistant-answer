# NexAi — Arabic-Native AI E-commerce Assistant

A production-grade, bilingual AI assistant for e-commerce platforms built on Spring Boot 4, LangChain4j 1.14, and a multi-tier routing architecture. Supports Arabic and English with intent-aware request analysis, RAG pipeline, PostgreSQL-backed memory, and full token analytics.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 25 |
| AI / LLM | LangChain4j 1.14.0, DeepSeek V4 Pro (via Qubrid API) |
| Embeddings | HuggingFace `sentence-transformers/all-MiniLM-L6-v2` (384 dims) |
| Vector DB | ChromaDB 0.5.23 |
| Relational DB | PostgreSQL 15+ + pgvector |
| Memory | PostgreSQL sliding-window conversation memory |
| Docs | SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`) |

---

## Architecture Overview

```
Request
  └── ChatService
        └── PlannerService
              ├── PlannerMemoryProcessor    (DB memory, no LLM)
              ├── RequestAnalyzer           (1 LLM call — enrich + analyze + intent)
              └── EcommerceStrategy
                    ├── isMultiStep=true → EcommerceExecutionPlanner → EcommercePlanExecutor
                    └── isMultiStep=false → EcommerceTierRouter
                          ├── Tier 0 — NO_LLM  → SQL direct
                          ├── Tier 1 — SIMPLE  → KnowledgeProcessor
                          ├── Tier 2 — MEDIUM  → Vector + LLM
                          └── Tier 3 — COMPLEX → Vector + LLM
                    └── MultiAgentCoordinator
                          ├── EcommerceEnglishTextAgent
                          │     └── EcommerceEnglishOrchestrator
                          │           ├── KnowledgeProcessor
                          │           ├── EnglishSortProcessor     (SQL)
                          │           ├── EcommerceFilterProcessor  (SQL)
                          │           ├── EnglishSegmentProcessor   (Vector + LLM)
                          │           └── EnglishSuggestionProcessor (fallback)
                          └── EcommerceArabicTextAgent
                                └── EcommerceArabicProcessingOrchestrator
                                      └── (same processors, Arabic prompts)
```

---

## Tier Routing

| Tier | Complexity | Search Types | LLM Calls | Source |
|---|---|---|---|---|
| 0 | NO_LLM | category, brand, price, hybrid, sort | 1 (analyzer only) | PostgreSQL |
| 1 | SIMPLE | knowledge, greeting | 1–2 | LLM only |
| 2 | MEDIUM | semantic, suggest | 2 | ChromaDB + LLM |
| 3 | COMPLEX | comparison | 2 | ChromaDB + LLM |
| ExecutionPlanner | COMPLEX + multiStep | multi-category | 2+ | Parallel steps |

---

## Search Types

| Type | Description |
|---|---|
| `category` | Filter by product category via SQL |
| `brand` | Filter by brand + optional category via SQL |
| `price` | Filter by min/max price range via SQL |
| `hybrid` | Combined brand + category + price via SQL |
| `sort` | Sort by price asc/desc via SQL |
| `semantic` | Vector similarity search + LLM answer |
| `comparison` | Vector search + LLM comparison answer |
| `knowledge` | General product knowledge via LLM |
| `suggest` | Fallback when no exact match — relaxed vector search |
| `greeting` | Hardcoded response, no LLM, no memory saved |

---

## Prerequisites

- Java 25+
- Maven 3.9+
- PostgreSQL 15+ with pgvector extension
- ChromaDB running on port 8000
- Qubrid API key

---

## Quick Start

### 1. Clone and build

```bash
git clone <repo-url>
cd ai-assistant-answer
mvn clean install -DskipTests
```

### 2. Configure `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_platform
    username: admin
    password: admin

langchain4j:
  chat-model:
    providers:
      qubrid:
        base-url: https://platform.qubrid.com/v1
        api-key: ${QUBRID_API_KEY}
    models:
      analyzer:
        model-name: deepseek-ai/DeepSeek-V4-Pro
        provider: qubrid
        max-tokens: 4096
      english:
        model-name: deepseek-ai/DeepSeek-V4-Pro
        provider: qubrid
        max-tokens: 1024
      arabic:
        model-name: deepseek-ai/DeepSeek-V4-Pro
        provider: qubrid
        max-tokens: 4096

app:
  business-strategy: ecommerce
```

### 3. Set environment variables

```bash
export QUBRID_API_KEY=your_api_key_here
```

### 4. Start ChromaDB

```bash
docker run -p 8000:8000 chromadb/chroma:0.5.23
```

### 5. Run the application

```bash
mvn spring-boot:run
```

---

## Project Structure

```
src/main/java/com/project/ai/
├── agents/                          # Multi-agent coordination
│   ├── MultiAgentCoordinator.java   # Routes to Arabic or English agent
│   ├── ecommerce/
│   │   ├── ArabicTextAgent.java
│   │   └── EnglishTextAgent.java
│   ├── AgentType.java
│   └── Language.java
│
├── config/                          # Spring configuration
│   ├── RagConfig.java               # ChromaDB, embedding model, chat models
│   ├── LangChain4jProperties.java   # Multi-model YAML config
│   ├── AppConfig.java               # Thread pool, async executor
│   ├── PromptKeys.java              # Prompt key constants
│   └── TokenTrackerFactory.java
│
├── controller/                      # REST endpoints
│   ├── ChatController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   ├── DashboardController.java
│   ├── PromptAdminController.java
│   ├── TokenTrackingController.java
│   ├── UserHistoryController.java
│   └── UploadController.java
│
├── dto/                             # Request/response DTOs
│   ├── MultimodalRequest.java       # Core chat request with token tracker
│   ├── MultimodalResponse.java      # Core chat response with matched products
│   ├── SearchIntent.java            # Parsed intent (type, category, brand, price)
│   ├── FilteredContext.java         # Vector search results after filtering
│   └── TokenTracker.java            # Per-request LLM call recorder
│
├── loader/
│   ├── PromptLoader.java            # Loads prompts from DB to cache at startup
│   └── CategoryLoader.java          # Loads category slugs and Arabic mappings
│
├── model/                           # JPA entities
│   ├── Product.java
│   ├── Category.java
│   ├── ConversationMemory.java      # pgvector memory table
│   ├── TokenRequestSummary.java
│   ├── TokenCallRecord.java
│   ├── prompt/BusinessPrompt.java
│   └── planner/                     # Planner model objects
│
├── processing/                      # Text processing pipeline
│   ├── planner/
│   │   ├── RequestAnalyzer.java     # LLM intent + complexity analysis
│   │   └── PlannerMemoryProcessor.java
│   └── text/
│       ├── InputProcessor.java      # Language detection + normalization
│       ├── arabic/                  # Arabic processors
│       └── english/                 # English processors
│           └── structure/           # Shared: FilterProcessor, MatchedIdsResolver
│
├── service/
│   ├── ChatService.java
│   ├── PlannerService.java
│   ├── ProductService.java          # CRUD + ChromaDB indexing
│   ├── MemoryService.java
│   ├── DashboardService.java
│   ├── TokenTrackerService.java
│   ├── SuggestionService.java
│   └── UserHistoryService.java
│
└── strategy/
    └── ecommerce/
        ├── EcommerceStrategy.java
        ├── EcommerceTierRouter.java
        └── executor/
            ├── EcommerceExecutionPlanner.java
            ├── EcommercePlanExecutor.java
            └── EcommerceAmbiguityResolver.java
```

---

## API Reference

### Chat

```
POST /api/v1/chat/{userId}
Body: { "question": "show me laptops" }
```

Response:
```json
{
  "question": "show me laptops",
  "type": "category",
  "answer": "MacBook Air M2 - $1199 - Laptops\nDell XPS 13 - $1099 - Laptops",
  "matchProducts": [
    { "id": "P004", "name": "MacBook Air M2", "price": "$1199.0", "category": "Laptops" }
  ],
  "language": "ENGLISH",
  "inputType": "TEXT",
  "responseTime": "2026-05-26T10:00:00",
  "suggestedOptions": []
}
```

### Products

```
POST   /api/v1/products              # Create single
POST   /api/v1/products/batch        # Batch create
POST   /api/v1/products/upload       # Upload JSON file
GET    /api/v1/products              # Paginated list
GET    /api/v1/products/{productId}  # Get by ID
PUT    /api/v1/products/{productId}  # Update
DELETE /api/v1/products/{productId}  # Delete + remove embedding
POST   /api/v1/products/index        # Re-index all active to ChromaDB
DELETE /api/v1/products/index        # Clear ChromaDB index
```

### Dashboard

```
GET /api/v1/dashboard/stats
GET /api/v1/dashboard/requests-by-type
GET /api/v1/dashboard/language-distribution
GET /api/v1/dashboard/response-time-trend?days=14
GET /api/v1/dashboard/recent-requests?size=10
```

### User History

```
GET    /api/v1/history/users
GET    /api/v1/history/{userId}
GET    /api/v1/history/stats
DELETE /api/v1/history/{userId}
DELETE /api/v1/history/{userId}/messages   Body: [1,2,3]
```

### Prompts Admin

```
GET    /api/admin/prompts
GET    /api/admin/prompts/{id}
POST   /api/admin/prompts
PUT    /api/admin/prompts/{promptKey}
PATCH  /api/admin/prompts/{id}/status?active=true
DELETE /api/admin/prompts/{id}
DELETE /api/admin/prompts/batch        Body: [1,2,3]
POST   /api/admin/prompts/reload-all
```

### Token Analytics

```
GET /api/v1/tokens/summary
GET /api/v1/tokens/requests
GET /api/v1/tokens/requests/{requestId}
GET /api/v1/tokens/users/{userId}/summary
GET /api/v1/tokens/users/{userId}/requests
```

---

## Database Setup

### Required extensions

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Seed prompts

All LLM prompts stored in `business_prompts` table (26 active). Required keys:

| Key | Purpose |
|---|---|
| `request_analyzer_english` | English request analysis |
| `request_analyzer_arabic` | Arabic request analysis |
| `execution_planner` | Multi-step execution planning |
| `knowledge_english/arabic` | General knowledge + greeting responses |
| `segment_english_*` | 7 English segment prompts |
| `segment_arabic_*` | 7 Arabic segment prompts |
| `suggestion_english/arabic` | Suggestion fallback |
| `clarification_english/arabic` | Ambiguity clarification |
| `intent_english/arabic` | Fallback intent extraction |

---

## ChromaDB Indexing Workflow

Always clear before re-indexing:

```
1. DELETE /api/v1/products/index    ← clear old embeddings
2. POST   /api/v1/products/index    ← re-index all active products
```

> Enable the ChromaDB volume in docker-compose before production use or data will be lost on restart.

---

## Memory System

Conversation memory stored in PostgreSQL with pgvector. Retrieval merges:

- **Semantic** — pgvector cosine similarity on `message_vector`
- **Recency** — latest N messages by `created_at DESC`

Truncated to 500 chars before passing to analyzer. Answers >300 chars summarized before saving. Greetings not saved.

---

## Supported Languages

- **English** — full support
- **Arabic (Modern Standard Arabic)** — full support, category name translation, Arabic-native prompts

---

## Configuration Reference

```yaml
app:
  business-strategy: ecommerce

langchain4j:
  chat-model:
    temperature: 0.7
    timeout: PT180S
  vector-store:
    chroma:
      default-min-score-threshold: 0.3
      top-k-max: 20
```

---

## Known Bugs Fixed

- `getString("id")` → `getString("productId")` in `FilterProcessor`, `MatchedIdsResolver`, `SuggestionService` — caused `matchedIds=[null]`
- `@Qualifier` ignored by `@RequiredArgsConstructor` — fixed with manual constructor
- `isComplexPlan` — only `isMultiStep=true` routes to `ExecutionPlanner`
- `ClarificationContext` NPE in `EcommercePlanExecutor` — null check added
- Native query `DATE()` not valid in JPQL — replaced with `LocalDateTime` parameter
- `Language` enum cast to `String` in `DashboardService` — fixed with `.toString()`