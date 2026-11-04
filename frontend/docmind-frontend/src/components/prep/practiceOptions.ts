import type { Mode, PracticeOptions, Setup } from '../../services/interviewApi';

export const scenarios = {
  EVERYDAY: 'Everyday conversation', WORKPLACE: 'Workplace conversation', PRESENTATION: 'Presenting an idea',
  DISCUSSION: 'Discussion & debate', CUSTOMER: 'Customer conversation',
};
export const communicationGoals = {
  CLARITY: 'Express ideas clearly', FLUENCY: 'Keep a conversation flowing', STRUCTURE: 'Organize an explanation',
  VOCABULARY: 'Use precise vocabulary', LISTENING: 'Respond & ask follow-up questions',
};
export const pronunciationFocuses = {
  SOUNDS: 'Individual sounds', WORD_STRESS: 'Word stress', SENTENCE_STRESS: 'Sentence stress & rhythm',
  INTONATION: 'Intonation', CONNECTED_SPEECH: 'Linking & connected speech',
};
export const exerciseFormats = {
  WORDS: 'Words & short phrases', SENTENCES: 'Short sentences', PASSAGE: 'A short passage', CUSTOM: 'My own text',
};
export const englishLevels = {
  BEGINNER: 'Basic — short, familiar phrases', INTERMEDIATE: 'Intermediate — connected ideas', ADVANCED: 'Advanced — detail & nuance',
};
export const styleLabels: Record<Mode, Record<Setup['style'], string>> = {
  INTERVIEW: { FRIENDLY: 'Friendly & encouraging', TECHNICAL: 'Technical deep dive', CHALLENGING: 'Challenging & probing' },
  COMMUNICATION: { FRIENDLY: 'Supportive conversation', TECHNICAL: 'Detailed language coaching', CHALLENGING: 'Stretch my speaking' },
  PRONUNCIATION: { FRIENDLY: 'Gentle guidance', TECHNICAL: 'Detailed sound & stress tips', CHALLENGING: 'More demanding drills' },
};
export function defaultSetups(): Record<Mode, Setup> {
  return {
    INTERVIEW: { mode: 'INTERVIEW', jobRole: 'Java / Spring Boot Developer', style: 'FRIENDLY', level: 'INTERMEDIATE', questionCount: 5 },
    COMMUNICATION: { mode: 'COMMUNICATION', style: 'FRIENDLY', level: 'INTERMEDIATE', questionCount: 5,
      practiceOptions: { scenario: 'EVERYDAY', communicationGoal: 'CLARITY' } },
    PRONUNCIATION: { mode: 'PRONUNCIATION', style: 'FRIENDLY', level: 'BEGINNER', questionCount: 5,
      practiceOptions: { pronunciationFocus: 'WORD_STRESS', exerciseFormat: 'SENTENCES' } },
  };
}
export function practiceDetails(mode: Mode, options?: PracticeOptions | null): [string, string][] {
  if (mode === 'INTERVIEW') return [];
  const entries: [string, string][] = mode === 'COMMUNICATION'
    ? [['Situation', scenarios[options?.scenario ?? 'EVERYDAY']], ['Speaking goal', communicationGoals[options?.communicationGoal ?? 'CLARITY']]]
    : [['Focus', pronunciationFocuses[options?.pronunciationFocus ?? 'WORD_STRESS']], ['Format', exerciseFormats[options?.exerciseFormat ?? 'SENTENCES']]];
  if (options?.topic?.trim()) entries.push([mode === 'COMMUNICATION' ? 'Topic' : 'Target words', options.topic.trim()]);
  return entries;
}
