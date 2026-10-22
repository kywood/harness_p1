# AGENTS

## AI Coding Rules
1. Only modify files in the `app/src/main/java` directory.
2. Each change must be in a single file, no more than 10 lines.
3. All changes must be accompanied by unit tests.

## Change Scope
- **Maximum**: 10 lines per change
- **Minimum**: 1 line per change
- **No**: External dependencies (e.g., new libraries without approval)

## Automated Tests
- **Unit tests**: For each component (e.g., `MagnetParser`, `TorrentDownloader`)
- **Integration tests**: For the end-to-end flow (magnet → download → stream)

## Security Rules
- Never store magnet links in plain text.
- Use Android's secure storage for sensitive data.
- Validate all inputs before processing.
