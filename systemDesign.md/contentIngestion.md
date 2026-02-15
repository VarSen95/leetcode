# Adyen Interview Prep — System Design (Documentation Experience)

**February 16, 2026**

---

## 1. Design a Documentation Ingestion Pipeline

*This is literally the job — most likely system design question.*

### The Problem

Adyen has documentation spread across multiple sources:
- **Headless CMS** (like Contentful) — where technical writers write guides
- **Git repositories** — where code samples and API specs live
- **OpenAPI specs** — JSON/YAML files that define API endpoints

These are all different formats, different update frequencies, different structures. You need to unify them into one searchable, accessible system.

### High-Level Architecture

```
SOURCES                    INGESTION              STORAGE              ACCESS
─────────                  ─────────              ───────              ──────

Headless CMS ──→                               ┌→ PostgreSQL ────→ REST API
                  [Kafka] ──→ Transformer ──→──┤→ Knowledge Graph → GraphQL API
Git Repos    ──→              (Java)           └→ ElasticSearch ──→ Search API
                                                                      ↑
OpenAPI Specs──→                                    Redis Cache ──────┘
```

### Your Elevator Pitch

> "I'd break this into four layers: ingestion, processing, storage, and access. Sources publish change events to Kafka. A Java transformation service consumes those events, cleans and structures the content, then writes to three stores — PostgreSQL as the source of truth, a knowledge graph for relationships between docs, and ElasticSearch for full-text search. The access layer exposes REST for simple lookups, GraphQL for complex relational queries, and a search API backed by ElasticSearch. Redis caches hot content."

---

## 2. Component Deep Dives

### 2.1 Kafka (Message Queue / Event Streaming)

**What it is:** A distributed message queue that sits between systems so they don't talk directly.

**Simple analogy:** A post office. Instead of delivering letters person-to-person, everyone drops letters at the post office, and recipients pick them up when ready.

```
Without Kafka:
Service A ──→ Service B    (A waits for B to respond)

With Kafka:
Service A ──→ [Kafka] ──→ Service B    (A drops message and moves on)
                      ──→ Service C    (multiple consumers read same message)
                      ──→ Service D
```

**Why Kafka here:**
- **Decoupling** — CMS doesn't know or care who processes its updates
- **Reliability** — if the transformer is down, messages wait in Kafka until it's back
- **Multiple consumers** — one event can trigger indexing, graph update, and quality check simultaneously
- **Ordering** — events for the same document are processed in order

**How it works in this system:**

Each source publishes an event when something changes:

```json
// Kafka topic: content-updates
{
    "source": "cms",
    "event": "document_updated",
    "documentId": "accept-payments-guide",
    "timestamp": "2026-02-16T10:30:00Z"
}
```

Multiple consumers read from this topic independently:
- Consumer 1: Transformation service (clean, structure, store)
- Consumer 2: Quality checker (validate links, check metadata)
- Consumer 3: Search indexer (update ElasticSearch)

### 2.2 Transformation Service (Java)

**This is the core of the role — where you'd spend most of your time.**

The service reads events from Kafka and:

1. **Extracts** — pulls content from CMS API, reads files from Git, parses OpenAPI YAML
2. **Transforms** — cleans HTML, normalizes formatting, extracts metadata
3. **Validates** — checks for broken links, missing required fields, schema compliance
4. **Structures** — converts everything into a unified document model

```java
// Unified document model — everything becomes this regardless of source
class DocumentNode {
    String id;
    String title;
    String content;              // cleaned, normalized
    String source;               // "cms", "git", "openapi"
    String product;              // "payments", "issuing", "platforms"
    String type;                 // "guide", "api-reference", "code-sample"
    Map<String, String> metadata;
    List<String> relatedDocIds;  // links to other docs
    Instant lastUpdated;
}
```

**Strategy pattern here too:**

```java
// Different extractors for different sources — same interface
interface ContentExtractor {
    DocumentNode extract(Event event);
}

class CmsExtractor implements ContentExtractor { ... }
class GitExtractor implements ContentExtractor { ... }
class OpenApiExtractor implements ContentExtractor { ... }

// Transformer picks the right extractor based on event source
ContentExtractor extractor = extractors.get(event.getSource());
DocumentNode doc = extractor.extract(event);
```

### 2.3 Storage Layer (Three Stores, Three Purposes)

Each store serves a different need:

| Store | Purpose | Query Example |
|-------|---------|---------------|
| **PostgreSQL** | Source of truth, structured data | "Get document by ID", "List all guides for Payments" |
| **Knowledge Graph** | Relationships between content | "Show me everything related to the /payments endpoint" |
| **ElasticSearch** | Full-text search with relevance ranking | "Search for 'refund webhook'" |

#### PostgreSQL — Source of Truth

```sql
CREATE TABLE documents (
    id VARCHAR PRIMARY KEY,
    title VARCHAR NOT NULL,
    content TEXT,
    source VARCHAR NOT NULL,        -- 'cms', 'git', 'openapi'
    product VARCHAR,                -- 'payments', 'issuing', 'platforms'
    type VARCHAR,                   -- 'guide', 'api-reference', 'code-sample'
    metadata JSONB,                 -- flexible key-value pairs
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE document_relationships (
    source_doc_id VARCHAR REFERENCES documents(id),
    target_doc_id VARCHAR REFERENCES documents(id),
    relationship_type VARCHAR,      -- 'references', 'depends_on', 'supersedes'
    PRIMARY KEY (source_doc_id, target_doc_id, relationship_type)
);

-- Indexes for common queries
CREATE INDEX idx_docs_product ON documents(product);
CREATE INDEX idx_docs_type ON documents(type);
CREATE INDEX idx_docs_updated ON documents(updated_at);
```

#### Knowledge Graph — Relationships

```
[Accept Payments Guide] ──references──→ [/payments API endpoint]
[/payments API endpoint] ──has_sample──→ [Java code sample]
[Java code sample] ──belongs_to──→ [Payments product]
[Accept Payments Guide] ──supersedes──→ [Old Payments Guide v1]
```

This lets you answer questions like:
- "Show me everything related to the Payments API" — graph traversal
- "What docs reference this endpoint?" — reverse edge lookup
- "What's the dependency chain for this guide?" — multi-hop traversal

**Adyen's approach:** Built on PostgreSQL with a custom Java graph engine (not Neo4j). They combine relational and graph properties in one system.

Alternatively, for this role, you might use **Neo4j** (mentioned in JD):

```cypher
// Neo4j Cypher query: find all content related to payments API
MATCH (doc:Document)-[:REFERENCES]->(api:APIEndpoint {path: "/payments"})
RETURN doc.title, doc.type

// Find orphan docs (no relationships — might be stale)
MATCH (doc:Document)
WHERE NOT (doc)-[]-()
RETURN doc.title, doc.updated_at
```

#### ElasticSearch — Full-Text Search

```json
// Document indexed in ElasticSearch
{
    "id": "doc-123",
    "title": "Accept payments online",
    "content": "Use the /payments endpoint to process online card payments...",
    "product": "payments",
    "type": "guide",
    "tags": ["checkout", "online", "integration", "cards"],
    "updated_at": "2026-02-15T10:00:00Z"
}
```

**Indexing strategy for relevance:**
- Boost title matches over content matches
- Boost recently updated docs
- Use synonyms ("refund" = "reversal" = "chargeback")
- Product-specific filtering

```json
// Search query with boosting
{
    "query": {
        "bool": {
            "should": [
                { "match": { "title": { "query": "refund webhook", "boost": 3 } } },
                { "match": { "content": { "query": "refund webhook", "boost": 1 } } },
                { "match": { "tags": { "query": "refund webhook", "boost": 2 } } }
            ],
            "filter": [
                { "term": { "product": "payments" } }
            ]
        }
    }
}
```

### 2.4 API Layer (REST + GraphQL)

#### REST — Simple Lookups

```
GET /docs/{id}                                → single document
GET /docs?product=payments&type=guide         → filtered list
GET /docs/{id}/related                        → related documents
GET /search?q=refund+webhook&product=payments → search with filters
```

#### GraphQL — Complex Relational Queries

```graphql
# Single query fetches doc + related docs + API endpoints
# Instead of 3 separate REST calls
query {
    doc(id: "doc-123") {
        title
        content
        product
        relatedDocs {
            title
            type
            url
        }
        apiEndpoints {
            path
            method
            description
        }
        codeSamples {
            language
            code
        }
    }
}
```

**Why GraphQL here:** Documentation is highly relational. A single guide references multiple API endpoints, has code samples in multiple languages, and links to other guides. GraphQL lets the client ask for exactly what it needs in one request.

#### Redis — Caching

Hot docs (like "Getting Started") get cached. Documentation doesn't change every minute, so caching works well:

```
Request → Check Redis (TTL: 5 min)
              ↓ cache miss
          Query PostgreSQL → Store in Redis → Return
              ↓ cache hit
          Return cached result
```

---

## 3. Monitoring & Quality

*Adyen values operational reliability — always mention monitoring.*

### Ingestion Monitoring
- Events per source per minute — detect if a source stops publishing
- Processing lag — time between event published and document stored
- Error rate — failed transformations, parse errors
- Dead letter queue — events that failed processing repeatedly

### Content Quality
- Broken link detection — scan all docs for dead links on every ingestion
- Stale content alerts — docs not updated in >6 months
- Missing metadata — docs without product tag, type, or description
- Orphan detection — docs with no relationships in the knowledge graph

### API Monitoring
- p50, p95, p99 latency for all endpoints
- Error rates by endpoint
- Cache hit ratio (Redis) — if too low, adjust TTL or pre-warm cache
- Search zero-result rate — queries that return nothing (indicates content gap)

### CI/CD Integration
- Docs validated on every Git push — schema check, link check, metadata check
- Automated tests for transformation logic
- Canary deployments for API changes

---

## 4. Knowledge Graph Deep Dive

### What Is a Knowledge Graph?

A database that stores data as **nodes** (things) and **edges** (relationships). Think social network map but for documentation.

### Documentation Knowledge Graph Schema

**Nodes:**

| Node Type | Examples | Properties |
|-----------|----------|------------|
| Document | "Accept Payments Guide" | title, content, type, product, updated_at |
| APIEndpoint | "/payments", "/refunds" | path, method, version, parameters |
| Product | "Payments", "Issuing" | name, description |
| CodeSample | "Java checkout example" | language, code, framework |
| Changelog | "API v70 release notes" | version, date, breaking_changes |

**Edges:**

| Edge Type | From → To | Meaning |
|-----------|-----------|---------|
| references | Document → APIEndpoint | "This guide explains this endpoint" |
| belongs_to | Document → Product | "This doc is part of Payments" |
| has_sample | APIEndpoint → CodeSample | "This endpoint has this code example" |
| depends_on | Document → Document | "Read this guide before this one" |
| supersedes | Document → Document | "This replaced the old version" |
| related_to | Document → Document | "Also relevant" |

### Why Graph Over Relational for This?

**Relational (SQL) approach:**
```sql
-- "Find all docs related to the Payments API, including their code samples"
-- Requires multiple JOINs, gets ugly fast
SELECT d.title, cs.language, cs.code
FROM documents d
JOIN document_relationships dr ON d.id = dr.source_doc_id
JOIN api_endpoints ae ON dr.target_doc_id = ae.id
JOIN endpoint_samples es ON ae.id = es.endpoint_id
JOIN code_samples cs ON es.sample_id = cs.id
WHERE ae.path = '/payments'
AND dr.relationship_type = 'references';
```

**Graph approach:**
```cypher
// Same query — much simpler, naturally follows relationships
MATCH (doc:Document)-[:REFERENCES]->(api:APIEndpoint {path: "/payments"})
OPTIONAL MATCH (api)-[:HAS_SAMPLE]->(sample:CodeSample)
RETURN doc.title, sample.language, sample.code
```

The graph approach is simpler to write, easier to extend (add new relationship types without schema changes), and faster for multi-hop traversals.

### Adyen's Approach vs Neo4j

| Aspect | Adyen's Custom Graph DB | Neo4j |
|--------|------------------------|-------|
| Built on | PostgreSQL + Java graph engine | Native graph storage |
| Query language | Custom SQL + Java | Cypher |
| Pros | Combines relational + graph, ACID, already in their stack | Purpose-built for graphs, powerful query language |
| Cons | Custom maintenance burden | Separate system to operate |
| Best for | Payment data (transactional + graph) | Pure graph use cases like documentation |

For the documentation role, Neo4j might be the better fit (and it's mentioned in the JD), but be ready to discuss both.

---

## 5. API Design for Documentation Access

### REST API Design

```
# Document operations
GET    /api/v1/docs                    → list all docs (paginated)
GET    /api/v1/docs/{id}               → get single doc
GET    /api/v1/docs/{id}/related       → get related docs
POST   /api/v1/docs                    → create doc (internal)
PUT    /api/v1/docs/{id}               → update doc (internal)

# Search
GET    /api/v1/search?q=refund&product=payments&type=guide

# Products
GET    /api/v1/products                → list products
GET    /api/v1/products/{id}/docs      → docs for a product

# API Endpoints
GET    /api/v1/endpoints               → list API endpoints
GET    /api/v1/endpoints/{path}/docs   → docs referencing an endpoint
```

### GraphQL Schema

```graphql
type Document {
    id: ID!
    title: String!
    content: String!
    product: Product
    type: DocType!
    relatedDocs: [Document]
    apiEndpoints: [APIEndpoint]
    codeSamples: [CodeSample]
    updatedAt: DateTime
}

type APIEndpoint {
    path: String!
    method: String!
    version: String
    docs: [Document]
    samples: [CodeSample]
}

type Query {
    doc(id: ID!): Document
    docs(product: String, type: DocType): [Document]
    search(query: String!, product: String): [SearchResult]
    endpoint(path: String!): APIEndpoint
}
```

### REST vs GraphQL — When to Use Each

| Use Case | Better Choice | Why |
|----------|--------------|-----|
| Simple doc lookup by ID | REST | Simple, cacheable, one resource |
| List docs with filters | REST | Standard pagination patterns |
| Full-text search | REST | ElasticSearch integration is straightforward |
| Doc + related docs + code samples | GraphQL | One query instead of 3 REST calls |
| API Explorer (interactive) | GraphQL | Client controls exactly what data it needs |
| Internal tools / dashboards | GraphQL | Flexible queries without backend changes |

---

## 6. Technologies Summary

| Technology | What It Does | Where It Fits |
|------------|-------------|---------------|
| **Kafka** | Message queue / event streaming | Ingestion — decouple sources from processing |
| **Java** | Application logic | Transformation service, API layer, graph engine |
| **PostgreSQL** | Relational database | Source of truth for document storage |
| **Neo4j** | Graph database | Knowledge graph — relationships between docs |
| **ElasticSearch** | Search engine | Full-text search with relevance ranking |
| **Redis** | In-memory cache | Cache hot docs, reduce DB load |
| **REST** | Standard API style | Simple lookups, search |
| **GraphQL** | Flexible query API | Complex relational queries |
| **Kubernetes** | Container orchestration | Deploy and scale all services |
| **Jenkins/GitLab CI** | CI/CD pipelines | Automated testing, validation, deployment |

---

## 7. How to Deliver This in the Interview

**Open with the four layers:**
> "I'd break this into four layers: ingestion, processing, storage, and access."

**Walk left to right:**
1. Sources publish to Kafka
2. Java transformer cleans and structures content
3. Three stores — PostgreSQL (truth), knowledge graph (relationships), ElasticSearch (search)
4. REST + GraphQL APIs with Redis caching

**Mention monitoring:**
> "For operational reliability, I'd track ingestion lag, content quality scores, API latency, and search zero-result rates."

**Connect to Adyen:**
> "I know Adyen builds in-house on open source — PostgreSQL, Java, their own graph engine. I'd follow that philosophy, building custom where it adds value and using proven open-source tools like Kafka and ElasticSearch where they're the right fit."

**If they ask about tradeoffs:**
- Kafka adds complexity but gives reliability and decoupling
- Three stores means three things to maintain, but each is optimized for its purpose
- GraphQL is powerful but harder to cache than REST
- Neo4j vs PostgreSQL graph — depends on query complexity and team expertise