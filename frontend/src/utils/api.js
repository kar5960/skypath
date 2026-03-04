// In Docker: nginx proxies /api/* to the backend, so we use a relative path (empty base).
// In local dev (npm start without Docker): set REACT_APP_API_URL=http://localhost:8080 in .env.local
const API_BASE = process.env.REACT_APP_API_URL || '';

export async function searchFlights({ origin, destination, date }) {
  const params = new URLSearchParams({ origin, destination, date });
  const response = await fetch(`${API_BASE}/api/flights/search?${params}`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
  });

  const data = await response.json();

  if (!response.ok) {
    // Backend returns { error: "INVALID_AIRPORT", message: "..." }
    throw new ApiError(data.message || 'Search failed', data.error, response.status);
  }

  return data;
}

export class ApiError extends Error {
  constructor(message, errorCode, status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
    this.name = 'ApiError';
  }
}
