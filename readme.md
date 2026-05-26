# NexAi — AI E-commerce Assistant

A production-grade, multi-language AI assistant for e-commerce platforms built on Spring Boot 4, LangChain4j, and a multi-tier routing architecture. Supports Arabic and English with intent-aware request analysis, RAG pipeline, and PostgreSQL-backed memory.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 25 |
| AI/LLM | LangChain4j 1.14.0, OpenAI-compatible API |
| LLM Providers | Qubrid (DeepSeek V4 Pro), NVIDIA NIM |
| Vector DB | ChromaDB |
| Relational DB | PostgreSQL + pgvector |
| Embeddings | HuggingFace `sentence-transformers/all-MiniLM-L6-v2` |
| Memory | PostgreSQL sliding-window conversation memory |

---

## Architecture Overview

```
Request
  └── ChatService
        └── PlannerService
              ├── PlannerMemoryProcessor    (DB memory, no LLM)
              ├── RequestAnalyzer           (1 LLM call — enrich + analyze + intent)
              └── BusinessStrategy
                    └── EcommerceStrategy
                          ├── EcommerceAmbiguityResolver   (clarification)
                          ├── EcommerceTierRouter
                          │     ├── Tier 0 — NO_LLM  → MultiAgentCoordinator → SQL direct
                          │     ├── Tier 1 — SIMPLE  → MultiAgentCoordinator → LLM
                          │     ├── Tier 2 — MEDIUM  → MultiAgentCoordinator → Vector + LLM
                          │     └── Tier 3 — COMPLEX → EcommerceExecutionPlanner → Multi-step
                          └── MultiAgentCoordinator
                                ├── EcommerceEnglishTextAgent
                                │     └── EcommerceEnglishOrchestrator
                                │           ├── KnowledgeProcessor
                                │           ├── SortProcessor      (SQL)
                                │           ├── EcommerceFilterProcessor (SQL)
                                │           └── SegmentProcessor   (Vector + LLM)
                                └── EcommerceArabicTextAgent
                                      └── EcommerceArabicOrchestrator
                                            └── (same processors, Arabic prompts)
```

---

## Tier Routing

| Tier | Complexity | Search Types | LLM Calls | Source |
|---|---|---|---|---|
| 0 | NO_LLM | sort, category, brand, price, hybrid | 1 (analyzer only) | PostgreSQL |
| 1 | SIMPLE | knowledge | 2 | LLM only |
| 2 | MEDIUM | semantic, comparison, suggest | 2 | Vector DB + LLM |
| 3 | COMPLEX | multi-step | 2+ | ExecutionPlanner |

---

## Prerequisites

- Java 25+
- Maven 3.9+
- PostgreSQL 15+ with pgvector extension
- ChromaDB running on port 8000
- API key for Qubrid or NVIDIA NIM

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
docker run -p 8000:8000 chromadb/chroma
```

### 5. Run the application

```bash
mvn spring-boot:run
```

---

## API Reference

### Chat

```
POST /api/v1/chat/{userId}
Content-Type: application/json

{
  "question": "show me laptops"
}
```

**Response:**
```json
{
  "question": "show me laptops",
  "type": "category",
  "answer": "MacBook Air M2 - $1199 - Laptops\nDell XPS 13 - $1099 - Laptops",
  "matchProducts": ["P004", "P005"],
  "language": "ENGLISH",
  "inputType": "TEXT",
  "responseTime": "2026-05-26T10:00:00",
  "suggestedOptions": []
}
```

### Products

```
POST /api/v1/products          — create single product
POST /api/v1/products/batch    — create multiple products
```

### Admin — Prompt Reload

```
POST /api/admin/prompts/reload           — reload all prompts from DB
POST /api/admin/prompts/categories/reload — reload category mappings
```

---

## Database Setup

### Required extensions

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Seed prompts

All LLM prompts are stored in the `business_prompts` table. Required keys:

| Key | Purpose |
|---|---|
| `request_analyzer_english` | English request analysis |
| `request_analyzer_arabic` | Arabic request analysis |
| `intent_english` | English intent extraction |
| `intent_arabic` | Arabic intent extraction |
| `knowledge_english` | English knowledge answers |
| `knowledge_arabic` | Arabic knowledge answers |
| `segment_english_*` | English segment prompts (7 types) |
| `segment_arabic_*` | Arabic segment prompts (7 types) |
| `suggestion_english` | English suggestion fallback |
| `suggestion_arabic` | Arabic suggestion fallback |
| `clarification_english` | English ambiguity clarification |
| `clarification_arabic` | Arabic ambiguity clarification |
| `execution_planner` | Multi-step execution planning |

---

## Supported Languages

- **English** — full support
- **Arabic (Modern Standard)** — full support, right-to-left, category translation

---

## Memory System

Conversation memory is stored in PostgreSQL with pgvector similarity search. Each AI response is summarized before saving to reduce token usage in subsequent requests.

```
Memory context: last 20 messages (configurable)
Similar messages: top 5 by vector similarity
Summarization: answers > 300 chars are summarized before saving
```

---

## Configuration Reference

```yaml
app:
  business-strategy: ecommerce   # active business strategy
  settings:
    memory:
      context: 20                # max recent messages to load
      similar: 5                 # max similar messages by vector

langchain4j:
  chat-model:
    temperature: 0.7
    timeout: PT180S              # 3 minutes
  vector-store:
    chroma:
      default-min-score-threshold: 0.3
      top-k-max: 20
```