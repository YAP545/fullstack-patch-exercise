-- H2-compatible task search query
-- Used by the Spring Data repository layer
--
-- Parameters:
--   :term   — search term wrapped in wildcards, e.g. '%api%'
--   :status — status filter or NULL for all statuses

-- FIX 1: Parenthesise the title/description OR so that archived = FALSE and the
--         status filter apply to both branches (AND binds tighter than OR).
-- FIX 4: ESCAPE '\' so %, _ and \ in :term are treated as literals.
-- FIX 1: Added id DESC as a deterministic tie-breaker for stable paging.
SELECT *
FROM tasks
WHERE archived = FALSE
  AND (   LOWER(title)       LIKE :term ESCAPE '\'
       OR LOWER(description) LIKE :term ESCAPE '\')
  AND (:status IS NULL OR status = :status)
ORDER BY created_at DESC, id DESC;
