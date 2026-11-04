# DocMind

DocMind is a full-stack document intelligence application that lets authenticated users upload documents, index their contents as embeddings, and ask questions through a retrieval-augmented generation (RAG) chat interface.

The project combines a Spring Boot backend, a React/Vite frontend, PostgreSQL, PGVector, and OpenAI models.

## Features

- JWT-based registration and login
- Protected application routes
- Single and multiple document uploads
- PDF and document parsing with Apache Tika
- Semantic document search using OpenAI embeddings and PGVector
- Retrieval-augmented chat responses with document citations
- Normal and streaming chat modes
- Persistent conversation and message history
- Zustand-based frontend state management
- Document search and chunk inspection
- OpenAPI/Swagger support

## Technology Stack

### Backend

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring AI 2.0
- OpenAI chat and embedding models
- PostgreSQL 16
- PGVector
- JJWT
- Apache Tika
- Springdoc OpenAPI
- Maven

### Frontend

- React 19
- TypeScript
- Vite
- Tailwind CSS 4
- Zustand
- Axios
- React Router
- React Markdown
- PrismJS
- Lucide React
- React Dropzone
- Framer Motion

## Architecture

```text
React + TypeScript frontend
        │
        │ HTTP requests with JWT authentication
        ▼
Spring Boot backend
  ├── AuthController
  ├── DocumentController
  ├── ChatController
  ├── DocumentParserService
  ├── DocumentIngestionService
  ├── DocumentMetadataService
  ├── RagService
  └── JpaChatMemory
        │
        ▼
PostgreSQL + PGVector
  ├── Users
  ├── Documents
  ├── Conversations
  ├── Chat messages
  └── Vector document chunks
        │
        ▼
OpenAI chat and embedding models
```

When a document is uploaded, the backend parses it, creates document metadata, splits the content into chunks, generates embeddings, and stores the chunks in PGVector.

When a user asks a question, `RagService` retrieves relevant document chunks, combines them with the question and conversation history, and sends the resulting prompt to the configured OpenAI chat model.

## Repository Structure

```text
.
├── src/main/java/com/substring/docmind/
│   ├── config/       Security, JWT, CORS, and application configuration
│   ├── controller/   Authentication, document, and chat REST controllers
│   ├── dto/          Request and response DTOs
│   ├── entity/       Users, documents, conversations, and messages
│   ├── exception/    Application exception handling
│   ├── repository/   Spring Data JPA repositories
│   └── service/      Parsing, ingestion, RAG, authentication, and chat services
├── src/main/resources/
│   ├── application.yaml
│   └── application-dev.yml
├── src/test/                         Backend tests
├── frontend/docmind-frontend/
│   ├── src/components/               Dashboard and application UI
│   ├── src/pages/                    Login and registration pages
│   ├── src/services/                 API service modules
│   ├── src/store/                    Zustand stores
│   ├── src/types/                    Frontend types
│   ├── src/App.tsx                   Routes and dashboard shell
│   └── vite.config.ts                Vite configuration and API proxy
├── docs/                             Architecture and implementation notes
├── docker-compose.yml                PostgreSQL + PGVector service
├── init.sql                          PostgreSQL extension initialization
├── pom.xml                           Maven configuration
├── mvnw                              Maven Wrapper for Linux/macOS
└── mvnw.cmd                          Maven Wrapper for Windows
```

## Prerequisites

Install the following before running the project:

- Java 21
- Node.js and npm
- Docker and Docker Compose
- An OpenAI API key
- Maven, or use the included Maven Wrapper

## Getting Started

### 1. Start PostgreSQL and PGVector

From the repository root:

```bash
docker compose up -d
```

The database runs with the following development settings:

```text
Host: localhost
Port: 5434
Database: docmind
Username: postgres
Password: postgres
```

The `init.sql` script enables the required PostgreSQL extensions:

- `vector`
- `uuid-ossp`

### 2. Configure Environment Variables

The development profile is enabled by default.

```bash
export OPENAI_API_KEY="your-openai-api-key"
```

Supported backend environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5434` | PostgreSQL port |
| `DB_NAME` | `docmind` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `OPENAI_API_KEY` | `demo-key` | OpenAI API key |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | OpenAI chat model |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | OpenAI embedding model |
| `JWT_SECRET` | Development placeholder | JWT signing secret |

For production deployments, replace all default credentials and use a strong `JWT_SECRET`.

### 3. Run the Backend

From the repository root:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend starts at:

```text
http://localhost:8081
```

To build and test the backend:

```bash
./mvnw clean verify
```

### 4. Run the Frontend

```bash
cd frontend/docmind-frontend
npm install
npm run dev
```

The frontend starts at:

```text
http://localhost:3000
```

The Vite development server proxies `/api` requests to:

```text
http://localhost:8081
```

Useful frontend commands:

```bash
npm run dev       # Start the development server
npm run build     # Type-check and create a production build
npm run lint      # Run Oxlint
npm run preview   # Preview the production build
```

## API Overview

The backend API uses the `/api/v1` base path.

### Authentication

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Protected endpoints require a JWT bearer token:

```text
Authorization: Bearer <accessToken>
```

### Documents

```text
POST   /api/v1/documents/upload
POST   /api/v1/documents/upload-multiple
GET    /api/v1/documents/user
GET    /api/v1/documents/{id}
DELETE /api/v1/documents/{id}
```

### Chat and Search

```text
POST /api/v1/chat/query
POST /api/v1/chat/stream
POST /api/v1/chat/search/similarity
```

A chat request can include:

```json
{
  "question": "What does this document explain?",
  "conversationId": "conversation-id",
  "documentId": "document-id",
  "topK": 5,
  "minSimilarity": 0.0
}
```

The normal chat endpoint returns an answer, conversation ID, citations, and response time.

The streaming endpoint returns chunked plain text.

## RAG Configuration

The development configuration is located at:

```text
src/main/resources/application-dev.yml
```

Default RAG settings include:

```text
Chunk size: 700
Top-k results: 5
Similarity threshold: 0.0
Vector index: HNSW
Distance metric: Cosine distance
Vector dimensions: 1536
Maximum file size: 25 MB
Maximum request size: 50 MB
```

The configured default models are:

```text
Chat model: gpt-4o-mini
Embedding model: text-embedding-3-small
```

## Documentation

Additional architecture and implementation notes are available in the `docs` directory:

- [`CHAT_MEMORY_DB_ARCHITECTURE.md`](docs/CHAT_MEMORY_DB_ARCHITECTURE.md)  
  Database-backed conversation memory and chat history design.

- [`USER_DOCUMENT_ISOLATION_ANALYSIS.md`](docs/USER_DOCUMENT_ISOLATION_ANALYSIS.md)  
  User document ownership, vector metadata filtering, and multi-tenant isolation recommendations.

- [`FRONTEND_BUILD_PROMPT.md`](docs/FRONTEND_BUILD_PROMPT.md)  
  Frontend architecture, API contracts, authentication, state management, and UI implementation notes.

- [`prompt.md`](docs/prompt.md)  
  Project prompt notes.

## Security Notes

Before deploying this project to a shared or production environment:

1. Replace the default PostgreSQL credentials.
2. Set a strong and private `JWT_SECRET`.
3. Provide a valid `OPENAI_API_KEY`.
4. Restrict the configured CORS origins.
5. Review document ownership checks on the server.
6. Ensure vector searches are filtered by the authenticated user.
7. Ensure conversations and messages are scoped to their owner.
8. Avoid using the development `demo-key` fallback.
9. Do not expose secrets in source control.

The document isolation design and recommended improvements are described in [`docs/USER_DOCUMENT_ISOLATION_ANALYSIS.md`](docs/USER_DOCUMENT_ISOLATION_ANALYSIS.md).

## License

No license file is currently included. Add a license before distributing or reusing this project publicly.