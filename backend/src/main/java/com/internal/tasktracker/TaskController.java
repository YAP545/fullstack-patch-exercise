package com.internal.tasktracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    // FIX 2: SLF4J logger replaces System.out.println
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // FIX 3: Validate status – return HTTP 400 instead of 500 on unknown value
        String normalizedStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status '" + status + "'. Must be one of: OPEN, IN_PROGRESS, DONE");
            }
        }

        // FIX 3: Validate page and pageSize – return HTTP 400 on out-of-range values
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        if (pageSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageSize must be > 0");
        }
        // FIX 3: Cap pageSize to prevent absurdly large in-memory slices
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

        // FIX 2: Removed Thread.sleep (was delaying short/blank queries up to 1 s).
        // FIX 2: Use SLF4J debug logging instead of System.out.println.
        String query = q == null ? "" : q.trim();
        log.debug("[TaskController] q=\"{}\" status={} page={} pageSize={}", query, normalizedStatus, page, pageSize);

        // FIX 4: Escape \, % and _ so they match literally inside LIKE … ESCAPE '\'
        String searchTerm = "%" + escapeLike(query.toLowerCase()) + "%";

        List<Task> allResults = taskRepository.searchTasks(searchTerm, normalizedStatus);

        // FIX 3: Use long arithmetic to avoid int overflow on the offset calculation
        long offsetL = (long) (page - 1) * pageSize;
        int start = (offsetL >= allResults.size()) ? allResults.size() : (int) offsetL;
        int end = (int) Math.min((long) start + pageSize, allResults.size());
        List<Task> pageResults = allResults.subList(start, end);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", pageResults);
        response.put("total", allResults.size());
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    /**
     * FIX 4: Escape backslash, percent and underscore so user-supplied text is
     * treated as literal characters in a LIKE pattern with ESCAPE '\'.
     */
    private static String escapeLike(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("%",  "\\%")
                .replace("_",  "\\_");
    }
}
