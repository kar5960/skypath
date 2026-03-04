# SkyPath — Flight Connection Search Engine

A full-stack flight itinerary search engine built with **Java 21 + Quarkus** (backend) and **React + Tailwind CSS** (frontend), containerised with Docker.

---

## Project Structure

```
skypath/
├── backend/                  Java 21 + Quarkus REST API
│   ├── src/main/java/com/skypath/
│   │   ├── model/            POJOs — Airport, Flight, FlightData, FlightSegment, Itinerary
│   │   ├── dto/              SearchRequest (validation), SearchResponse (serialization)
│   │   ├── service/          Interfaces — DataLoaderService, FlightSearchService
│   │   │   └── impl/         DataLoaderServiceImpl, FlightSearchServiceImpl
│   │   ├── controller/       FlightSearchResource (JAX-RS endpoints)
│   │   ├── exception/        Custom exceptions + GlobalExceptionMapper
│   │   └── filter/           CorsFilter
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                 React + Tailwind CSS
│   ├── src/
│   │   ├── components/       SearchForm, ResultsList, ItineraryCard
│   │   ├── hooks/            useFlightSearch
│   │   └── utils/            api.js
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
├── flights.json              Dataset (25 airports, ~260 flights)
└── README.md
```

---

## Running the Application

**Prerequisites:** Docker and Docker Compose

```bash
git clone https://github.com/kar5960/skypath.git
cd skypath
docker-compose up
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui |
| Health check | http://localhost:8080/api/flights/health |

> First run takes 5–10 minutes to download base images and Maven dependencies. Subsequent runs use the Docker layer cache and start in seconds.

---

## API Reference

### `GET /api/flights/search`

```
GET /api/flights/search?origin=JFK&destination=LAX&date=2024-03-15
```

| Parameter | Type | Required | Example |
|-----------|------|----------|---------|
| `origin` | 3-letter IATA code | ✓ | `JFK` |
| `destination` | 3-letter IATA code | ✓ | `LAX` |
| `date` | `YYYY-MM-DD` | ✓ | `2024-03-15` |

### `POST /api/flights/search`

```json
{ "origin": "JFK", "destination": "LAX", "date": "2024-03-15" }
```

### Success Response `200`

```json
{
  "totalResults": 3,
  "itineraries": [
    {
      "stops": 0,
      "totalDurationMinutes": 375,
      "totalDurationFormatted": "6h 15m",
      "totalPrice": 299.00,
      "segments": [
        {
          "flightNumber": "SP101",
          "airline": "SkyPath Airways",
          "originCode": "JFK",
          "originCity": "New York",
          "destinationCode": "LAX",
          "destinationCity": "Los Angeles",
          "departureTime": "2024-03-15T08:30:00",
          "arrivalTime": "2024-03-15T11:45:00",
          "departureTimezone": "America/New_York",
          "arrivalTimezone": "America/Los_Angeles",
          "durationMinutes": 375,
          "durationFormatted": "6h 15m",
          "price": 299.00
        }
      ],
      "layovers": []
    }
  ]
}
```

### Error Response `400`

```json
{ "error": "INVALID_AIRPORT", "message": "Unknown airport code: XXX" }
```

> The API was developed and tested interactively using **Swagger UI** at `/swagger-ui`, which is bundled via the `quarkus-smallrye-openapi` extension. All endpoints, parameters, and error responses are documented there.

---

## Architecture & Design Decisions

### Why Java 21 + Quarkus over Spring Boot or other frameworks?


Quarkus specifically was chosen over Spring Boot for three reasons:

1. **Cloud-native design** — Quarkus is built for containerised deployments. Its CDI container, JAX-RS implementation, and build pipeline are all optimised for Docker. The `quarkus-maven-plugin` produces a _fast-jar_ with separated dependency layers, making Docker layer caching highly effective.

2. **Built-in Swagger UI** — the `quarkus-smallrye-openapi` extension bundles OpenAPI spec generation and a Swagger UI with zero extra configuration. This made API development and testing significantly faster — every endpoint was testable interactively without writing a separate test client.

3. **Startup performance** — Quarkus starts in under 2 seconds on JVM, which matters for the Docker health check flow. The frontend only starts once the backend passes its health check; a slow-starting backend delays the whole stack.

Quarkus also supports native image compilation via GraalVM, which produces a near-zero startup binary. We chose the standard JVM fast-jar instead because native compilation takes 10–15 minutes (impractical for `docker-compose up` in an assessment context), requires GraalVM instead of a standard JDK, and adds complexity around reflection configuration for Jackson deserializers.

### Why REST over GraphQL?

GraphQL's main advantage is letting clients request exactly the fields they need from a flexible schema — particularly useful when multiple clients (mobile, web, third-party) need different shapes of the same data, or when a frontend is doing many small queries that benefit from batching.

For SkyPath, the use case is a single, well-defined query: given origin, destination, and date, return a list of itineraries. The response shape is fixed and the frontend consumes all of it. GraphQL would add schema definition overhead, a resolver layer, and query parsing complexity with no real benefit. REST with a clean JSON contract is the right tool here.

### Why BFS? Why not Dijkstra or DFS?

The search problem is: find all valid paths from airport A to airport B with at most 2 intermediate stops, where each edge (flight) has connection time constraints.

**BFS was chosen for three reasons:**

1. **Multi-source seeding** — there are often multiple flights from the origin airport on the search date (e.g. JFK has 8+ departures). BFS naturally handles this by seeding the queue with all valid first-leg flights simultaneously, exploring all paths level by level rather than going deep on one route first.

2. **Natural depth control** — BFS expands paths one hop at a time. Enforcing `MAX_STOPS = 2` is trivial: simply don't enqueue paths that already have 2 segments. With DFS, you'd need explicit backtracking and depth tracking.

3. **Finds all valid itineraries** — unlike Dijkstra which finds the single shortest path, BFS explores all reachable paths. We need all valid itineraries, not just the cheapest or fastest, so we can sort and present them to the user.

**Time complexity per query:**

Let `F` = total flights (~260), `A` = airports (~25), `k` = max stops (2), `F/A` = avg flights per airport (~10).

**Worst case — O(F^(k+1)):** If all flights happened to depart from the same origin airport, the seed phase picks up all F flights, and each expansion checks F candidates again. With k=2 this gives O(F³) = O(260³) ≈ 17.5 million operations. In practice this never happens with a realistic dataset.

**Average case — O((F/A)^(k+1)):** In a balanced dataset where flights are spread across airports, each level of BFS checks ~F/A candidates. This gives O((F/A)³) = O(10³) ≈ **1,000 operations** — imperceptible.

**With layover filtering:** `isValidConnection()` rejects most candidates immediately (wrong timing, layover too short or too long), so the real number of paths that survive to the next level is far smaller than F/A. In practice per-query BFS completes in **well under 1,000 connection checks** on this dataset.

**Space complexity:** O(F/A)^k for the BFS queue — at peak, the queue holds the surviving partial paths from the current expansion level.


### Data Loading & Indexing

`flights.json` is read once at startup via `@Startup @PostConstruct` and two indexes are built:

- `airportByCode` — `Map<String, Airport>` for O(1) airport resolution by IATA code
- `flightsByOrigin` — `Map<String, List<Flight>>` for O(1) retrieval of all flights departing from any airport

Without these indexes, every BFS expansion would require a linear scan over all 260 flights to find the next legs — O(F) per hop instead of O(1).

### Timezone Handling

Every local time string in the dataset (e.g. `"2024-03-15T08:30:00"`) is converted to UTC using the airport's `timezone` field before any duration or comparison calculation. This correctly handles:

- Cross-timezone US routes — JFK `08:30 EST` departs before LAX `11:45 PST` arrives (correct: 5h 15m flight)
- International routes — SFO `PST` to NRT `JST` (+17 hours difference)
- Date-line crossings — SYD departure `09:00 AEDT` and LAX arrival `06:00 PST` appear reversed in local time. UTC conversion resolves this: SYD `09:00 AEDT` = `22:00 UTC (prev day)`, LAX `06:00 PST` = `14:00 UTC` — a correct 16-hour flight. A `+1 day` guard is retained as a safety net for genuinely malformed data.

**Connection rules** enforced in `isValidConnection()`:
- Minimum layover: 45 min (domestic) or 90 min (international)
- Maximum layover: 6 hours
- "Domestic" = all three airports across both connecting segments are in the same country

### Interface-Driven Service Layer

`DataLoaderService` and `FlightSearchService` are defined as interfaces with single implementations. This keeps the controller decoupled from implementation details — the `FlightSearchResource` controller only depends on the `FlightSearchService` interface, not on how the search is implemented. Swapping to a database-backed loader or a different search algorithm requires no controller changes.

### Data Quirks Handled

The dataset contains two intentional imperfections:

- **Flight SP995** has a typo origin `"JKF"` instead of `"JFK"`. `resolveSegment()` looks up both airports for every flight — if either is missing, the flight is silently skipped with a `WARN` log. Nothing crashes.
- **Three flights** have `price` as a JSON string (`"289.00"`) rather than a number. A custom `FlexibleDoubleDeserializer` handles string, integer, and float transparently via `Double.parseDouble()`.

---

### Frontend

React with Tailwind CSS. State is isolated in the `useFlightSearch` hook — `SearchForm` handles only its own validation state (touched fields, inline errors) and calls `onSearch` on submit. The hook manages loading, error, and results state independently.

In Docker, Nginx serves the pre-built React static files and reverse-proxies `/api/*` to the backend container. The browser never makes a cross-origin request, so no CORS configuration is needed in production. Locally, the `.env.local` file sets `REACT_APP_API_URL=http://localhost:8080` so the dev server hits the backend directly.

---

## Infrastructure

**Docker multi-stage builds** — the backend `Dockerfile` uses two stages: Maven compiles and packages in stage one, then only the Quarkus fast-jar is copied into a lean `eclipse-temurin:21-jre-alpine` runtime image, excluding Maven, source code, and build tooling from the final image. The frontend follows the same pattern — `node:20-alpine` builds the React app, `nginx:alpine` serves it.

**Health check** — `docker-compose.yml` pings `/api/flights/health` every 10 seconds with a 30-second grace period for JVM startup. The frontend container only starts once the backend is healthy.

**Volume mount** — `flights.json` is mounted from the project root into the backend container at `/data/flights.json` as read-only. No data is baked into the image; the file can be swapped without rebuilding.

---

## Tradeoffs & Assumptions

**In-memory data vs a database** — For 260 flights and 25 airports, loading everything into memory at startup is perfectly reasonable. It keeps the stack simple — no database container, no ORM configuration, no migrations. The tradeoff is that this won't scale: a real flight search engine has millions of flights and needs a proper database. For this prototype, the simplicity is worth it.

**BFS over Dijkstra** — BFS was the right choice here because we need *all* valid itineraries, not just the single cheapest or shortest one. Dijkstra finds one optimal path; BFS explores everything within the depth limit. With a maximum of 2 stops and ~260 flights the search space is small enough that BFS runs in milliseconds. Dijkstra would be more appropriate if we were optimising for a single best result across a very large graph.

**REST over GraphQL** — GraphQL shines when different clients need different shapes of the same data, or when a frontend is doing many small queries that benefit from batching. SkyPath has one query type, one response shape, and one client — REST is simpler and equally capable here. GraphQL would be worth considering if we added more query types (e.g. airport lookup, price calendar) or a mobile client with different data needs.

**JVM fast-jar over Quarkus native image** — Quarkus can compile to a native binary via GraalVM with near-instant startup. We chose the standard JVM build because native compilation takes 10–15 minutes (impractical for `docker-compose up`), requires GraalVM instead of a standard JDK, and needs extra configuration for Jackson reflection. The JVM fast-jar starts in under 2 seconds which is sufficient here.

---

## What I Would Improve With More Time

- **Caching Layer** — Implement Redis to cache frequent search results (e.g., JFK → LAX). Since flight schedules don't change every second, this would significantly reduce CPU load on the Quarkus backend.

- **Result grouping** — Currently all itineraries are shown in one sorted list. I would split them into tabs: *Direct*, *1 Stop*, and *2 Stops* — this is how real flight search UIs work and makes results much easier to scan

- **Tests** — Unit tests for `FlightSearchServiceImpl` covering the layover boundary conditions (exactly 45 and 90 minutes), the dateline case, and the data quirks (SP995 typo, flexible price parsing); integration tests via RestAssured for the full API endpoints

- **Advanced Filtering** — Add frontend toggles to filter by "Cheapest," "Non-stop only," or specific "Airlines."

- **Airport autocomplete** — Replace the plain text input with a typeahead dropdown that shows the city name and full airport name as you type the IATA code, reducing user errors

- **Persistence & Scalability** — Move the flight data from a JSON file to a Graph Database like Neo4j. This would allow for much more complex route finding (3+ stops) and faster querying as the dataset grows to millions of flights.

- **Rate limiting** — The search endpoint currently has no request throttling. A real deployment would add rate limiting per IP or API key to prevent abuse and protect the backend from being overwhelmed by too many concurrent searches

- **Pagination** — Hub airports like JFK can return a large number of itineraries; `limit` and `offset` query parameters would make the response more manageable



---

## Test Cases

| # | Input | Expected |
|---|-------|----------|
| 1 | `JFK → LAX, 2024-03-15` | Direct flights and multi-stop itineraries |
| 2 | `SFO → NRT, 2024-03-15` | International route — 90 min minimum layover enforced |
| 3 | `BOS → SEA, 2024-03-15` | No direct flight — connections via ORD, DEN, DFW, ATL |
| 4 | `JFK → JFK, 2024-03-15` | `400` — same origin and destination |
| 5 | `XXX → LAX, 2024-03-15` | `400` — unknown airport code |
| 6 | `SYD → LAX, 2024-03-15` | Date-line crossing resolved correctly via UTC conversion |
