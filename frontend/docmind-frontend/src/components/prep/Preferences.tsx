import { Moon, Sun, Volume2, VolumeX } from 'lucide-react';
import { usePreferences } from '../../store/preferencesStore';

export default function Preferences() {
  const { theme, sound, toggleTheme, toggleSound } = usePreferences();
  return <div className="workspace-preferences" aria-label="Workspace preferences">
    <button type="button" onClick={toggleSound} aria-label="Recording sound effects" aria-pressed={sound} title={`Recording sounds ${sound ? 'on' : 'off'}`}>
      {sound ? <Volume2 size={17} /> : <VolumeX size={17} />}
    </button>
    <button type="button" onClick={toggleTheme} aria-label="Night mode" aria-pressed={theme === 'dark'} title={theme === 'dark' ? 'Switch to day mode' : 'Switch to night mode'}>
      {theme === 'dark' ? <Moon size={17} /> : <Sun size={17} />}<span>{theme === 'dark' ? 'Night' : 'Day'}</span>
    </button>
  </div>;
}
