# Verification — 27 September 2026

## Passed

- Java 21 / Spring Boot backend compilation and startup against the existing PostgreSQL 18 database with pgvector.
- 12 automated backend tests, zero failures: six HTTP integration tests, three AI orchestration tests, two WAV validation tests, one multipage report test.
- Frontend TypeScript check and Vite production build.
- Frontend lint: no errors; two pre-existing warnings in legacy ChatView/ChunksView. New PrepRoom screens have no lint warnings.
- Browser registration and login against the real backend; dashboard/setup navigation; starting a pronunciation session and sending a typed answer. Desktop and 390px mobile layouts checked, including the live feedback view: no horizontal overflow or browser errors. Verification account signed out after checks.
- Supplied Gemini key authenticated successfully. The account lists `gemini-3.8-flash` and `gemini-embedding-001`.
- Uploaded a dedicated verification-company.txt, parsed/indexed it with real Gemini embeddings, and retrieved its passages from PGVector.
- Gemini generated a technical interview question referring to the verification company’s document-search workflow, idempotency, and transaction boundaries.
- Sent a 13.5-second synthetic WAV recording through the authenticated audio endpoint. Gemini returned its transcript, audio observations, and a follow-up question.
- Background feedback completed, with an evidence-based response evaluation and speech notes.
- Downloaded the live session’s PDF report. Separately rendered and inspected the PDF pagination test’s first page.

## Local configuration

- The supplied Gemini key is stored only in the ignored root `.env`; it is not in frontend code or documentation.
- A fresh random JWT signing secret was generated locally.
- Existing database settings were retained. Before adding interview tables, a full local backup was made at `target/preproom-before-migration.sql` (ignored).
- Verification data belongs to a dedicated verification account and does not change existing users’ documents.
- Application URLs: http://localhost:3000 and backend http://localhost:8081.

## Scope limits

- Voice is recorded in turns, not continuous duplex streaming. Browser speech synthesis handles question playback.
- The live audio check used synthetic speech. Physical microphone permissions, device-specific recording, and speaker playback were not tested on the user’s hardware.
- Speech assessments are AI coaching estimates, not calibrated pronunciation certification.
- No public deployment, load test, or external penetration test was performed.
- PDF output is optimized for English; unsupported font characters are replaced with a placeholder.

