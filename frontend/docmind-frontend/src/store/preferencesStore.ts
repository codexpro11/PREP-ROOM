import { create } from 'zustand';

type Theme = 'light' | 'dark';
function read(key: string) {
  try { return localStorage.getItem(key); } catch { return null; }
}
function save(key: string, value: string) {
  try { localStorage.setItem(key, value); } catch { /* Preferences still work for this visit. */ }
}
const initialTheme: Theme = document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light';

export const usePreferences = create<{
  theme: Theme;
  sound: boolean;
  toggleTheme: () => void;
  toggleSound: () => void;
}>((set) => ({
  theme: initialTheme,
  sound: read('preproom-sound') !== 'off',
  toggleTheme: () => set(state => {
    const theme = state.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    save('preproom-theme', theme);
    return { theme };
  }),
  toggleSound: () => set(state => {
    save('preproom-sound', state.sound ? 'off' : 'on');
    return { sound: !state.sound };
  }),
}));
