# Testing Guide

## Current test scope

Unit tests currently cover:
- command aggregation and streaming behavior
- shell send/resize/error mapping
- session state transitions and backend delegation
- backend config defaults and validation helpers
- client exception mapping paths

## Run all tests

```powershell
.\gradlew.bat test
```

## Run a specific test class

```powershell
.\gradlew.bat test --tests "*DefaultSshClientTest*"
```

## Logs during tests

Tests now include `slf4j-simple` at runtime, so SLF4J messages are shown instead of NOP warnings.

By default, test output is quiet (`sshClientCore.test.logLevel=off` and no standard streams).

### Run with verbose output

Use the dedicated task to see all test results, full exceptions, and debug logs:

```powershell
.\gradlew.bat testVerbose
```

**Note:** You'll see ERROR logs from certain tests (e.g., `CommandExecutorTest`, `DefaultShellSessionTest`).
These are **expected** — those tests intentionally trigger failures to validate error handling behavior.

## Recommended next layers

1. Contract tests: same behavior matrix for both backends.
2. Integration tests: execute against a real SSH server (container/local VM).
3. Performance smoke tests: many short-lived commands and shell open/close loops.
4. Regression suite for host-key verification edge cases.

