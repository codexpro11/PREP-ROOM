# Practice setup by mode

Research and implementation: 12 October 2026.

The setup now asks about the skill being practised. Interview settings describe hiring context; language settings describe the situation, speaking goal, and English comfort level. The same selections are stored with the session and supplied to question generation, recorded-audio coaching, and final feedback.

## Research and design decisions

The British Council organizes speaking lessons by language level and practical situations. Its examples range from meeting people and checking understanding to giving advice and responding to disagreement. That supports a situation selector and a speaking goal rather than a required occupation for communication practice. These sources inform the design; the exact five options below are product choices, not an official curriculum.

- [British Council: speaking practice](https://learnenglish.britishcouncil.org/free-resources/speaking)

The Council of Europe separates phonological competence into overall control, sound articulation, and prosody. It emphasizes intelligibility rather than imitation of a native accent. That supports choosing sounds, stress, rhythm, intonation, or connected speech without requiring an accent to be removed.

- [Council of Europe: phonological competence](https://www.coe.int/en/web/common-european-framework-reference-languages/phonological-competence)

British Council pronunciation guidance covers pitch, stress, rhythm, and connected speech. TeachingEnglish describes weak forms and linking, and exercises using recordings and short phrases. The exercise selector therefore distinguishes words, sentences, a short passage, and the learner's own material. Rhythm and intonation need sentence context, so word-only drills are unavailable for those focuses.

- [British Council: improving intonation](https://www.britishcouncil.org/voices-magazine/how-english-learners-can-improve-intonation)
- [TeachingEnglish: connected speech](https://www.teachingenglish.org.uk/professional-development/teachers/knowing-subject/connected-speech-part-1)

## Available settings

| Mode | Required choices | Optional context | Session length |
|---|---|---|---|
| Mock interview | Target job role, career experience, coaching style | Indexed company or job-description document | Questions |
| Communication | Conversation situation, speaking goal, English comfort, coaching style | Topic to talk about | Conversation turns |
| Pronunciation | Pronunciation focus, exercise format, English comfort, coaching style | Specific sounds or words; read-aloud text is required for “My own text” | Exercises |

Communication situations: everyday conversation, workplace conversation, presenting an idea, discussion/debate, customer conversation. Goals: clear expression, conversational flow, organized explanation, precise vocabulary, and responding with follow-up questions. The final goal is conversational responding; this app does not claim to measure listening comprehension from a typed prompt.

Pronunciation focuses: individual sounds, word stress, sentence stress/rhythm, intonation, linking/connected speech. Formats: words/short phrases, short sentences, a short passage, or custom text. Custom text is limited to 1200 characters and presented in short excerpts by the coach. Target words or topics are limited to 200 characters.

English comfort choices describe basic phrases, connected ideas, or nuanced speech. They are self-selected difficulty settings, not a placement test or CEFR certification. Language modes offer supportive, detailed, or more demanding coaching; “Technical deep dive” remains specific to interviews.

## Behavior and compatibility

- Each mode keeps its own form draft, including style, level, and length, while switching modes on this page. Reloading the setup resets unsaved drafts. Settings for a created session survive reloading it.
- Job role and company documents are only attached to interview sessions. The API ignores stale interview context in language requests. It discards options from other modes and fills mode-appropriate defaults for older clients.
- `practiceOptions` is an optional object added to setup and detail responses. Existing six-field interview requests remain supported. The existing `jobRole` response field holds a descriptive session title for language practice, keeping history and older consumers readable.
- Questions, audio observations, and final feedback receive the same stored options. Custom material is untrusted data, never an instruction to the model.
- Pronunciation setup explains the need for a recording. Typed answers remain available for wording practice; text-only feedback cannot claim spoken fluency or pronunciation. Language feedback excludes STAR scores. Pronunciation feedback and its PDF omit grammar scores because reading a supplied passage does not demonstrate sentence construction.
- Browser speech playback remains the existing synthetic English voice. No accent matching or phoneme measurement is promised.

## Database rollout

The only schema addition is a nullable text column storing the validated options as JSON. Local development uses the existing Hibernate `ddl-auto=update` setting. No existing sessions need to be rewritten. For a production database using `ddl-auto=validate`, include this additive PostgreSQL migration in the release process before starting the updated application:

```sql
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS practice_options text;
```

No production database migration or public deployment was performed by this change.

## Verification on 12 October 2026

| Boundary | Result | Evidence |
|---|---|---|
| Form and summary | Passed | Interview-only fields remain in mock interviews; communication and pronunciation show their own fields, summaries, coaching labels, and length labels |
| Mode switching | Passed | Presentation/structure/topic, advanced level, detailed coaching, and 3 turns retained after switching to pronunciation and interview |
| Pronunciation validation | Passed | Empty custom text disables Start; intonation switches a word-only drill to sentences and disables the word-only option |
| Keyboard and mobile | Passed | Enter starts a pronunciation session; communication and custom-text pronunciation have no horizontal overflow at a 390px viewport |
| Browser → API → saved data → reloaded UI | Passed with fixture AI | Both language sessions were created in an isolated H2 database and reloaded with their selected settings and descriptive titles; pronunciation retained its supplied passage |
| Coaching inputs | Passed with mocked model | Captured question, audio, and feedback prompts receive the selected settings; language questions skip company retrieval |
| Feedback UI | Passed with fixture AI | Communication omits STAR; pronunciation shows exercise completion and fluency, omits grammar/STAR, and marks typed pronunciation as unassessed |
| PDF reports | Passed | Extracted communication/pronunciation reports include practice settings and omit inapplicable interview metrics |

Final checks: `./mvnw.cmd test` passed **25 tests, zero failures/errors**. `npm run build` passed TypeScript and the production build. `npm run lint` passed with two pre-existing warnings in the legacy ChatView and ChunksView screens; the edited screens have no lint warnings.

Browser checks used `BrowserVerificationApplication` on port 18081 and a separate Vite preview on port 3100. This test launcher uses H2, fixture AI, and a local allowed-origin setting; it does not load the root `.env` or call Gemini. The existing running application was left intact. Live Gemini output, physical microphone behavior, and the production PostgreSQL migration were not exercised in this run.

Captured previews: [communication](../frontend/docmind-frontend/output/playwright/communication-setup.png), [pronunciation](../frontend/docmind-frontend/output/playwright/pronunciation-setup.png), [communication mobile](../frontend/docmind-frontend/output/playwright/communication-mobile.png), [pronunciation mobile](../frontend/docmind-frontend/output/playwright/pronunciation-mobile.png).
