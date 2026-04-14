# Design Decisions

## 2026-04: Interactive Terminal UX

### Context

The project includes a desktop `main` used as an operator/test terminal while the library itself targets app integration (including Android).

### Decisions

1. `exec` in the sample terminal uses live streaming instead of blocking output.
2. Two shell modes are exposed in `main`:
   - `shell`: line mode (safe default)
   - `shellraw`: raw mode (desktop TTY only)
3. `shellraw` exits by either:
   - local escape (`Ctrl+]`)
   - typing `exit` and Enter
4. ANSI stripping is applied in line mode, but not in raw mode.
5. For JSch command compatibility, sample command execution may use `sh -lc` wrapping.

### Rationale

- Better UX in local terminal testing.
- Avoid user confusion with hidden prompts / control sequences.
- Preserve terminal-native behavior in raw mode.
- Keep Android path separate from desktop TTY concerns.

### Android Impact

- Raw mode is not an Android concern; UI should render output and send input events.
- Added API helpers to reduce boilerplate in app code.

