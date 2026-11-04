# PrepRoom — AI interview and communication coach

A Java 21 / Spring Boot 4.1 application built on the existing DocMind document reader. React 19 + TypeScript provides the practice workspace. Gemini (`gemini-3.8-flash` by default) generates questions, understands recorded speech, and produces coaching feedback.

## What is implemented

- Account registration and JWT login; private company-document library.
- Mock interviews by job role, experience level, interviewer style, and 3–15 questions.
- Communication practice by conversation situation, speaking goal, English comfort level, and optional topic.
- Pronunciation practice by sounds, stress, rhythm, intonation, or linking; choose words, sentences, a passage, or your own text.
- Mode-specific setup, saved session settings, and tailored coaching/feedback. See [practice options and research](docs/PRACTICE_OPTIONS.md).
- Company PDF/DOCX/TXT/MD/CSV ingestion using Tika → chunks → Gemini embeddings → a persistent local vector index.
- Follow-up questions grounded in the selected document and the candidate's previous answers.
- Voice answers recorded as mono PCM WAV; Gemini transcribes and provides audio observations. Browser speech synthesis reads questions aloud.
- Persistent sessions and transcripts, resumable from My sessions; duplicate answer retries are idempotent.
- Background feedback with relevance, grammar, applicable fluency/STAR estimates, strengths, improvements, example answer, and practice plan.
- Optional Kafka-backed feedback jobs with a transactional database outbox, retries, and a dead-letter topic.
- PDF feedback/transcript download.
- Ownership enforcement on interview, transcript, feedback, document lookup, and document deletion endpoints.

This implementation uses recorded, turn-by-turn voice over authenticated REST. It does **not** implement continuous duplex audio, live partial transcription, STOMP, Groq, or Deepgram. Gemini is the only AI provider required, following the requested model preference. Browser voice availability varies by operating system/browser. Microphone access requires HTTPS or localhost.

Pronunciation feedback is qualitative coaching based on audio, not a calibrated phoneme-level assessment. Typed answers never receive pronunciation or spoken-fluency scores. STAR is only relevant when a behavioral response was requested. Audio is discarded by this app after processing; transcripts and coaching notes are persisted. Gemini receives audio and selected document passages under your Google API account's data policies.

## Run locally

Prerequisites: Java 21, Node.js 22.12+ (or a newer supported release), PostgreSQL, and a Gemini API key with access to the configured chat and embedding models. Maven is included through the wrapper.

For local development, document embeddings are saved to `./data/vector-store.json` (override with `VECTOR_STORE_FILE`). Keep this file with your PostgreSQL data to retain document search across restarts; it contains extracted document text. This local store is intended for a single backend instance, not production scale. No pgvector extension is required for uploads.

1. Copy `.env.example` to `.env` in the repository root. Fill in `GEMINIAI_API_KEY`, a private `JWT_SECRET` of at least 32 bytes, and `DB_PASSWORD`. Do not commit `.env`.
2. For the bundled database, run `docker compose up -d`. It uses localhost port **5434** and database **docmind**. The example `DB_URL` matches this. To reuse your existing DocMind database, set `DB_URL` to its existing address instead. Keep the embedding model consistent for an already populated local index.
3. In the repository root, start the backend:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. In a second terminal:

   ```powershell
   cd frontend/docmind-frontend
   npm ci
   npm run dev
   ```

5. Open http://localhost:3000, create an account, optionally upload company documents, and start a practice session. Backend: http://localhost:8081. API explorer: http://localhost:8081/swagger-ui/index.html.

Spring reads `.env` as an optional properties file: use unquoted `NAME=value` entries. Environment variables can also be configured in your IDE or hosting platform. The existing database default remains `localhost:5432/ai_doc`; `.env.example` overrides it for Docker. Existing document/chat tables are preserved. New tables are `interviews` and `interview_transcripts`; feedback is stored with its owning interview. Local schema creation uses Hibernate `ddl-auto=update`. Back up existing databases before adopting changes; for production, use reviewed schema migrations and set `ddl-auto=validate`.

## Configuration

| Variable | Purpose |
|---|---|
| `GEMINIAI_API_KEY` | Gemini API key, server-side only |
| `GEMINIAI_CHAT_MODEL` | Defaults to `gemini-3.8-flash`; must support text and audio input in your account |
| `GEMINIAI_EMBEDDING_MODEL` | Defaults to `gemini-embedding-001` |
| `DB_URL` | JDBC PostgreSQL URL |
| `DB_USER` | Defaults to `postgres` |
| `DB_PASSWORD` | Database password; required |
| `JWT_SECRET` | Private signing secret, at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins; defaults to `http://localhost:3000` |
| `KAFKA_ENABLED` | Defaults to `false`; enables durable Kafka interview-feedback processing |
| `KAFKA_BOOTSTRAP_SERVERS` | Defaults to `localhost:9092`; use `kafka:19092` for clients inside Compose |
| `KAFKA_ANALYSIS_TOPIC` | Defaults to `preproom.interview.analysis.v1` |
| `KAFKA_ANALYSIS_GROUP` | Defaults to `preproom-feedback-v1`; use the same group across workers |
| `KAFKA_TOPIC_PARTITIONS` / `KAFKA_TOPIC_REPLICAS` | Defaults to 3 / 1; configure replicas for your broker cluster |
| `KAFKA_OUTBOX_POLL_MS` | Defaults to 2000; interval for publishing queued jobs |
| `KAFKA_RETRY_ATTEMPTS` / `KAFKA_RETRY_DELAY_MS` | Defaults to 2 retries after the initial attempt / 2000 ms |

The frontend proxies `/api` to localhost:8081 during development. For deployment, serve `frontend/docmind-frontend/dist` using a static web server with SPA fallback and proxy `/api` to the Spring Boot service. Preserve HTTPS for the microphone and restrict CORS to your actual frontend origin. Do not put any provider key in a `VITE_` variable. The app has not been deployed or load-tested; add production migrations, API quotas/rate limits and your operational monitoring before a public rollout.

## Interview API

All routes below require `Authorization: Bearer <JWT>`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/interviews` | Create session and first question |
| GET | `/api/v1/interviews` | Latest 100 owned sessions |
| GET | `/api/v1/interviews/{id}` | Owned session, transcript, feedback |
| POST | `/api/v1/interviews/{id}/answers` | `{text, requestId}`; UUID reused for retries |
| POST | `/api/v1/interviews/{id}/audio` | Multipart `file` (mono 16-bit PCM WAV, ≤3 minutes) and UUID `requestId` |
| POST | `/api/v1/interviews/{id}/finish` | Finish after at least one answer; generate or retry feedback |
| GET | `/api/v1/interviews/{id}/report` | PDF after feedback is ready |

Setup body: `jobRole`, `mode` (`INTERVIEW`, `COMMUNICATION`, `PRONUNCIATION`), `style` (`FRIENDLY`, `TECHNICAL`, `CHALLENGING`), `level` (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`), `questionCount` (3–15), optional `documentId`.

Session states: `IN_PROGRESS` → `COMPLETED` (question limit) → `ANALYZING` → `ANALYZED` or `ANALYSIS_FAILED`. Ending early enters analysis directly. Retry failed analysis using `/finish`. An interrupted/stale analysis can be retried after five minutes; attempt IDs prevent old jobs overwriting new feedback. With Kafka disabled, background jobs run in-process. With Kafka enabled, jobs are stored durably and delivered through Kafka. A failed question generation rolls back the new turn; the UI retains the answer for retry. Raw provider errors are not exposed to users.

## Kafka feedback queue

1. Set `KAFKA_ENABLED=true` in `.env` and keep `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` for a backend running on your host.
2. Start the local broker with `docker compose --profile kafka up -d kafka` (start `postgres` separately if needed).
3. Restart the backend and finish a practice session. The existing UI polls the session until feedback is ready; no API or frontend change is needed.

`POST /api/v1/interviews/{id}/finish` stores the analysis attempt and an `analysis_outbox` row in the same database transaction. A relay publishes a JSON message containing only `id`, `userId`, and `attempt`, keyed by interview ID, and removes the outbox row after broker acknowledgement. Transcripts and audio are not placed on Kafka. The consumer loads the owned session from the database and generates feedback. Broker outages retain the outbox row for later delivery, including after an app restart.

Delivery is at least once. Completed or superseded attempts are ignored and feedback writes check both attempt ID and session status under a row lock. A rebalance can still cause concurrent AI calls, but only the current analyzing attempt can store feedback. Exhausted AI retries mark the current attempt `ANALYSIS_FAILED` and send the message to `<analysis-topic>.DLT`; malformed messages also go there. A user can retry through `/finish`, which creates a fresh attempt. Replaying a failed attempt from DLT does not restart analysis; request a fresh attempt instead. Monitor the DLT and outbox backlog. Keep Kafka enabled while draining pending work; disabling it pauses the relay and leaves existing Kafka jobs queued.

The Compose broker uses the [official Apache Kafka image](https://kafka.apache.org/41/getting-started/docker/) and a persistent volume, and binds its plaintext host port to localhost. It is a single-broker development setup. For deployment, configure broker authentication/TLS using `spring.kafka` properties, topic replication, and reviewed schema migrations for `analysis_outbox`. The dependency follows [Spring Boot Kafka configuration](https://docs.spring.io/spring-boot/reference/messaging/kafka.html). Existing document uploads and chat remain synchronous.

## Verification

```powershell
.\mvnw.cmd test
cd frontend/docmind-frontend
npm run build
npm run lint
```

Backend tests use an isolated H2 database and mocked AI; they do not spend API credits. They cover JWT-protected session lifecycle, ownership, request validation, answer deduplication, transaction rollback on AI failure, retrieval filters, text-only feedback honesty, and multipage PDF output. Practice-mode checks also cover saved settings, custom-text validation, mode isolation, audio prompt configuration, language feedback, and PDF settings/metrics. Kafka tests use an embedded broker and cover outbox delivery, broker failure retention, transaction rollback, duplicate/stale attempts, AI retries, failure recovery, and malformed-message dead-letter routing. The suite contains 25 tests and requires no external Kafka or Docker service. Earlier live verification passed against the existing PostgreSQL/pgvector database and Gemini: registration/login, document parsing and embedding, document-grounded questions, recorded-audio transcription/coaching, asynchronous feedback, and PDF download. The audio check used synthetic speech, not a human microphone recording. See docs/VERIFICATION.md for those earlier checks and docs/PRACTICE_OPTIONS.md for the current practice-mode verification.

## Implementation map

- `src/main/java/com/substring/docmind/interview/`: entities, REST, Gemini orchestration, background analytics, WAV validation, reports.
- `src/main/java/com/substring/docmind/kafka/`: durable analysis outbox, relay, Kafka consumer, topic creation, retry/dead-letter handling.
- `frontend/docmind-frontend/src/components/prep/`: dashboard, authentication, setup, company library, interview room, feedback.
- `frontend/docmind-frontend/src/hooks/useVoiceRecorder.ts`: microphone lifecycle and WAV encoding.
- Existing document ingestion, authentication, and document assistant APIs remain available.
- Original project notes: `docs/DOCMIND_ORIGINAL_README.md`.

Provider references: [Gemini audio understanding](https://ai.google.dev/gemini-api/docs/audio), [Spring AI Google GenAI](https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html).

