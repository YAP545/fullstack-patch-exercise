# NOTES.md

## Summary of changes

1. **SQL operator precedence** (`TaskRepository.java`, `search_tasks.sql`, `task_search_package.sql`): Parenthesised the title/description OR so `archived = FALSE` and the status filter apply to both branches. Added `id DESC` tie-breaker in all three files.

2. **Thread.sleep / logging** (`TaskController.java`): Removed `Thread.sleep` (was delaying short queries up to 1 s). Replaced `System.out.println` with SLF4J `Logger` at `DEBUG` level.

3. **Input validation** (`TaskController.java`): Invalid `status` → HTTP 400 instead of 500. `page < 1` or `pageSize <= 0` → HTTP 400. `pageSize` capped at 100. Offset uses `long` to avoid int overflow.

4. **LIKE wildcard escaping** (`TaskController.java`, all SQL files): `\`, `%`, `_` in user input are escaped before the LIKE term is built; all queries use `ESCAPE '\'`.

5. **`useTasks.js` race & stuck loading**: `AbortController` cancels stale in-flight requests. `setError(null)` clears the previous error at fetch start. `setLoading(false)` is in a `finally` block so it always fires (skipped on abort). Signal wired through `api.js`.

6. **Debounce + page reset** (`App.jsx`, `api.js`): 300 ms `useDebounce` on the search input. Page resets to 1 on query or status change. Removed `console.log` from `api.js`.

## What I chose not to change

Paging is still done in Java via `subList` — see *Biggest remaining risk*. No auth added. H2 DB, schema/data SQL, folder structure, and startup commands are unchanged.

## Biggest remaining risk

**In-memory paging (`subList`).** Every request loads all matching rows into heap before slicing. The fix is `LIMIT`/`OFFSET` in SQL, but that was left out to keep the diff small.

## Assumptions

Status values are `OPEN`, `IN_PROGRESS`, `DONE`. Oracle PL/SQL is a reference artefact only. CORS is restricted to `http://localhost:5173`.

## Tools / AI used

Antigravity AI (Google DeepMind) generated and applied all changes. Every change was reviewed by Yash Patil before committing.
