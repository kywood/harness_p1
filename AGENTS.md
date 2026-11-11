# AGENTS

## AI Coding Rules
1. Engine code (`app/src/main/java/**/engine/`): each change must be in a single file, no more than 10 lines.
2. UI code (`app/src/main/java/**/ui/`, `MainActivity.kt`, `app/src/main/res`): one screen or component per change; no line limit.
3. All logic changes must be accompanied by unit tests (`app/src/test`, `app/src/androidTest`).
4. Team conventions: `CONVENTIONS.md` (common), `PM.md`, `planning/CLAUDE.md`, `design/CLAUDE.md`, `program/CLAUDE.md`.

## Change Scope
- **Engine maximum**: 10 lines per change
- **Minimum**: 1 line per change
- **No**: External dependencies (e.g., new libraries without approval)

## Automated Tests
- **Unit tests**: For each component (e.g., `MagnetParser`, `TorrentDownloader`)
- **Integration tests**: For the end-to-end flow (magnet → download → stream)

## Security Rules
- Never store magnet links in plain text.
- Use Android's secure storage for sensitive data.
- Validate all inputs before processing.
