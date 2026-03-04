import { useState, useCallback } from 'react';
import { searchFlights } from '../utils/api';

export function useFlightSearch() {
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastQuery, setLastQuery] = useState(null);

  const search = useCallback(async ({ origin, destination, date }) => {
    setLoading(true);
    setError(null);
    setResults(null);
    setLastQuery({ origin, destination, date });

    try {
      const data = await searchFlights({ origin, destination, date });
      setResults(data);
    } catch (err) {
      setError(err.message || 'An unexpected error occurred');
    } finally {
      setLoading(false);
    }
  }, []);

  const reset = useCallback(() => {
    setResults(null);
    setError(null);
    setLastQuery(null);
  }, []);

  return { results, loading, error, lastQuery, search, reset };
}
