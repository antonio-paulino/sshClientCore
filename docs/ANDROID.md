# Android Integration Notes

This document explains the UX and API decisions for Android usage.

## Decision Summary

- Use `executeStreaming` (or `executeLive`) for command output in real time.
- Use `openShell()` for interactive terminal sessions.
- Do **not** rely on desktop raw-terminal logic (`TerminalBuilder`) on Android.
- Keep shell I/O lifecycle-aware (ViewModel scope + cancellation on `onCleared`).

## Why Raw Terminal Logic Is Desktop-Only

The sample `main` includes desktop-oriented behavior (`shellraw`) based on local TTY access.
Android apps do not provide a local console/TTY in the same way.

For Android:
- Collect `ShellSession.output` and render text in UI.
- Send typed input using `shell.send(...)` or `sendLine(...)`.
- Map special keys from your UI to bytes (or use `TerminalKey`).

## Recommended Android Architecture

- **ViewModel** owns `SshClient`, `SshSession`, `ShellSession`.
- **UI** only sends user intents (connect, send command, send key, disconnect).
- **StateFlow** exposes terminal text / connection state.
- Cancel shell output collection when screen is disposed.

## Suggested Flow

1. Connect with `SshConfig`.
2. Open shell with `session.openShell()`.
3. Start output collection with `consumeOutput(...)`.
4. Send user text with `sendLine(...)`.
5. Send control keys with `sendKey(...)`.
6. On screen close: cancel output job, close shell, disconnect session.

## API Helpers Added

- `SshSession.executeLive(...)` for command streaming callbacks.
- `ShellSession.consumeOutput(...)` for lifecycle-scoped output collection.
- `ShellSession.sendLine(...)` for line-oriented input.

These helpers exist to simplify app code while keeping low-level API access available.

