import api from './api';

export type Mode = 'INTERVIEW' | 'COMMUNICATION' | 'PRONUNCIATION';
export type Status = 'IN_PROGRESS' | 'COMPLETED' | 'ANALYZING' | 'ANALYZED' | 'ANALYSIS_FAILED';
export interface PracticeOptions {
  scenario?: 'EVERYDAY' | 'WORKPLACE' | 'PRESENTATION' | 'DISCUSSION' | 'CUSTOMER' | null;
  communicationGoal?: 'CLARITY' | 'FLUENCY' | 'STRUCTURE' | 'VOCABULARY' | 'LISTENING' | null;
  pronunciationFocus?: 'SOUNDS' | 'WORD_STRESS' | 'SENTENCE_STRESS' | 'INTONATION' | 'CONNECTED_SPEECH' | null;
  exerciseFormat?: 'WORDS' | 'SENTENCES' | 'PASSAGE' | 'CUSTOM' | null;
  topic?: string | null; customText?: string | null;
}
export interface Setup {
  jobRole?: string; mode: Mode; style: 'FRIENDLY' | 'TECHNICAL' | 'CHALLENGING';
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'; questionCount: number; documentId?: string;
  practiceOptions?: PracticeOptions | null;
}
export interface Summary {
  id: string; jobRole: string; mode: Mode; style: Setup['style']; status: Status;
  answered: number; questionCount: number; createdAt: string;
}
export interface Turn {
  id: string; speaker: 'AI' | 'CANDIDATE'; content: string; audioFeedback: string | null;
  audioSeconds: number | null; sources: string | null; createdAt: string;
}
export interface Feedback {
  relevanceScore: number; grammarScore: number; fluencyScore: number | null; starScore: number | null;
  summary: string; strengths: string[]; improvements: string[]; practicePlan: string[];
  exampleAnswer: string; pronunciationNotes: string;
}
export interface Interview extends Setup {
  id: string; jobRole: string; status: Status; documentName: string | null; createdAt: string; endedAt: string | null;
  transcripts: Turn[]; feedback: Feedback | null;
}
export const interviewApi = {
  list: async () => (await api.get<Summary[]>('/interviews')).data,
  create: async (setup: Setup) => (await api.post<Interview>('/interviews', setup)).data,
  get: async (id: string) => (await api.get<Interview>(`/interviews/${id}`)).data,
  answer: async (id: string, text: string, requestId: string) =>
    (await api.post<Interview>(`/interviews/${id}/answers`, { text, requestId })).data,
  audio: async (id: string, audio: Blob, requestId: string) => {
    const form = new FormData(); form.append('file', audio, 'answer.wav'); form.append('requestId', requestId);
    return (await api.post<Interview>(`/interviews/${id}/audio`, form, { headers: { 'Content-Type': 'multipart/form-data' } })).data;
  },
  finish: async (id: string) => (await api.post<Interview>(`/interviews/${id}/finish`)).data,
  report: async (id: string) => {
    const { data } = await api.get<Blob>(`/interviews/${id}/report`, { responseType: 'blob' });
    const url = URL.createObjectURL(data);
    const a = document.createElement('a'); a.href = url; a.download = `preproom-${id}.pdf`; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  },
};
export function errorMessage(error: unknown): string {
  const e = error as { response?: { data?: { message?: string } }; message?: string };
  return e.response?.data?.message || 'We couldn’t complete that request. Check your connection and try again.';
}
