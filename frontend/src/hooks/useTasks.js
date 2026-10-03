import { useState, useEffect } from 'react';
import { fetchTasks } from '../api';

// FIX 5: All three bugs fixed:
// (a) setLoading(false) is now called in a finally block so it always runs,
//     including on error.
// (b) setError(null) is called at the start of every request to clear any
//     previous error before the new fetch begins.
// (c) AbortController cancels the in-flight request on cleanup so a slow older
//     response can never overwrite a newer one. AbortError is ignored.
export function useTasks(query, status, page, pageSize) {
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    let aborted = false;

    setLoading(true);
    setError(null); // FIX 5b: clear previous error before each request

    fetchTasks({ query, status, page, pageSize, signal: controller.signal })
      .then((data) => {
        setTasks(data.items);
        setTotal(data.total);
      })
      .catch((err) => {
        // FIX 5c: ignore AbortError – it's intentional cancellation, not an error
        if (err.name === 'AbortError') {
          aborted = true;
          return;
        }
        setError(err.message);
      })
      .finally(() => {
        // FIX 5a: always clear loading, but skip if this request was aborted
        if (!aborted) {
          setLoading(false);
        }
      });

    // FIX 5c: cancel the in-flight request when deps change or component unmounts
    return () => {
      controller.abort();
    };
  }, [query, status, page, pageSize]);

  return { tasks, total, loading, error };
}
