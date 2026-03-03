package com.skypath.service.impl;

import com.skypath.exception.InvalidAirportException;
import com.skypath.exception.InvalidSearchException;
import com.skypath.model.Airport;
import com.skypath.model.Flight;
import com.skypath.model.FlightSegment;
import com.skypath.model.Itinerary;
import com.skypath.service.DataLoaderService;
import com.skypath.service.FlightSearchService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * BFS-based itinerary search supporting direct flights and up to 2 stops.
 *
 * Key design decisions:
 * - All connection validation is done in UTC to correctly handle timezone
 * crossings
 * and date-line crossings (e.g. SYD→LAX where arrival appears before departure
 * in local time)
 * - Flights are filtered to those departing on the requested local date at the
 * origin airport
 * - Layover rules:
 * - Domestic (same country for both arriving and departing flights): min 45,max 6h
 * - International: min 90 min, max 6h
 * - Max 2 stops (3 segments)
 */
@ApplicationScoped
public class FlightSearchServiceImpl implements FlightSearchService {

    private static final Logger LOG = Logger.getLogger(FlightSearchServiceImpl.class);

    private static final long MIN_LAYOVER_DOMESTIC_MINUTES = 45;
    private static final long MIN_LAYOVER_INTERNATIONAL_MINUTES = 90;
    private static final long MAX_LAYOVER_MINUTES = 360; // 6 hours
    private static final int MAX_STOPS = 2;

    private static final DateTimeFormatter LOCAL_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Inject
    DataLoaderService dataLoader;

    @Override
    public List<Itinerary> search(String origin, String destination, String date) {
        // --- Input validation ---
        validateInputs(origin, destination, date);

        // These calls are purely for validation — throws InvalidAirportException if not found
        dataLoader.findAirport(origin).orElseThrow(() -> new InvalidAirportException(origin));
        dataLoader.findAirport(destination).orElseThrow(() -> new InvalidAirportException(destination));

        LocalDate searchDate = parseDate(date);

        LOG.debugf("Searching %s -> %s on %s", origin, destination, date);

        List<Itinerary> results = new ArrayList<>();

        // BFS: each queue entry is a partial itinerary (list of segments built so far)
        // We explore paths up to MAX_STOPS connections
        Queue<List<FlightSegment>> queue = new ArrayDeque<>();

        // Seed with all valid first-leg flights from origin on the search date
        List<Flight> firstLegs = dataLoader.getFlightsByOrigin(origin);
        for (Flight flight : firstLegs) {
            FlightSegment seg = resolveSegment(flight);
            if (seg == null)
                continue;
            if (!departsOnDate(seg, searchDate))
                continue;

            List<FlightSegment> path = new ArrayList<>();
            path.add(seg);

            if (flight.getDestination().equals(destination)) {
                results.add(new Itinerary(path));
            } else if (path.size() <= MAX_STOPS) {
                queue.add(path);
            }
        }

        // Expand partial paths
        while (!queue.isEmpty()) {
            List<FlightSegment> currentPath = queue.poll();

            if (currentPath.size() > MAX_STOPS)
                continue; // safety guard

            FlightSegment lastSeg = currentPath.get(currentPath.size() - 1);
            String currentAirportCode = lastSeg.getFlight().getDestination();

            // Avoid cycles: collect visited airports
            Set<String> visitedAirports = new HashSet<>();
            visitedAirports.add(origin);
            currentPath.forEach(s -> visitedAirports.add(s.getFlight().getDestination()));

            List<Flight> nextLegs = dataLoader.getFlightsByOrigin(currentAirportCode);
            for (Flight nextFlight : nextLegs) {
                String nextDest = nextFlight.getDestination();

                // Skip cycles
                if (visitedAirports.contains(nextDest) && !nextDest.equals(destination))
                    continue;
                // Don't revisit the final destination mid-path (already reached origin)
                if (nextDest.equals(origin))
                    continue;

                FlightSegment nextSeg = resolveSegment(nextFlight);
                if (nextSeg == null)
                    continue;

                // Validate connection timing
                if (!isValidConnection(lastSeg, nextSeg))
                    continue;

                List<FlightSegment> newPath = new ArrayList<>(currentPath);
                newPath.add(nextSeg);

                if (nextDest.equals(destination)) {
                    results.add(new Itinerary(newPath));
                } else if (newPath.size() <= MAX_STOPS) {
                    queue.add(newPath);
                }
            }
        }

        // Sort by total duration ascending (shortest trip first)
        results.sort(Comparator.comparingLong(Itinerary::getTotalDurationMinutes));

        LOG.infof("Found %d itineraries for %s -> %s on %s", results.size(), origin, destination, date);
        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a Flight into a FlightSegment by attaching Airport objects
     * and computing UTC-normalised ZonedDateTimes from the local time strings.
     *
     * Returns null if airport data is missing or times cannot be parsed.
     */
    private FlightSegment resolveSegment(Flight flight) {
        Optional<Airport> originOpt = dataLoader.findAirport(flight.getOrigin());
        Optional<Airport> destOpt = dataLoader.findAirport(flight.getDestination());

        if (originOpt.isEmpty() || destOpt.isEmpty()) {
            LOG.warnf("Missing airport data for flight %s (%s -> %s); skipping",
                    flight.getFlightNumber(), flight.getOrigin(), flight.getDestination());
            return null;
        }

        Airport origin = originOpt.get();
        Airport dest = destOpt.get();

        try {
            ZonedDateTime depUtc = toUtc(flight.getDepartureTime(), origin.getTimezone());
            ZonedDateTime arrUtc = toUtc(flight.getArrivalTime(), dest.getTimezone());

            // Timezone conversion handles all cases correctly:
            //
            // - Overnight domestic (e.g. LAX->JFK dep 22:00 PST, arr 06:30 EST next day):
            // Both times include their date, so UTC conversion is unambiguous.
            //
            // - Date-line crossing (e.g. SYD->LAX dep 09:00 AEDT, arr 06:00 PST "same
            // date"):
            // SYD 2024-03-15T09:00 AEDT = 2024-03-14T22:00 UTC
            // LAX 2024-03-15T06:00 PST = 2024-03-15T14:00 UTC → arrUtc > depUtc ✓
            // The local arrival time appears earlier on the calendar, but UTC is correct
            // because the dataset stores the actual local calendar date at each airport.
            //
            // The +1 day guard below is a safety net for malformed data only.
            if (arrUtc.isBefore(depUtc)) {
                LOG.warnf("Flight %s: arrival UTC (%s) is before departure UTC (%s); shifting +1 day",
                        flight.getFlightNumber(), arrUtc, depUtc);
                arrUtc = arrUtc.plusDays(1);
            }

            return new FlightSegment(flight, origin, dest, depUtc, arrUtc);

        } catch (DateTimeParseException e) {
            LOG.warnf("Could not parse times for flight %s: %s", flight.getFlightNumber(), e.getMessage());
            return null;
        }
    }

    /**
     * Converts a local datetime string + timezone name to a UTC ZonedDateTime.
     */
    private ZonedDateTime toUtc(String localTimeStr, String timezone) {
        LocalDateTime ldt = LocalDateTime.parse(localTimeStr, LOCAL_DT_FMT);
        ZoneId zone = ZoneId.of(timezone);
        return ldt.atZone(zone).withZoneSameInstant(ZoneId.of("UTC"));
    }

    /**
     * Checks if a flight's departure (at its origin's local timezone) falls on the
     * requested date.
     */
    private boolean departsOnDate(FlightSegment seg, LocalDate searchDate) {
        ZoneId originZone = ZoneId.of(seg.getOriginAirport().getTimezone());
        LocalDate flightLocalDate = seg.getDepartureUtc()
                .withZoneSameInstant(originZone)
                .toLocalDate();
        return flightLocalDate.equals(searchDate);
    }

    /**
     * Validates that a connection between two segments respects layover rules.
     *
     * Rules:
     * - Next flight must depart AFTER previous flight arrives (in UTC)
     * - Layover >= minimum (45 min domestic, 90 min international)
     * - Layover <= 6 hours
     * - Connection airport must be the same (no airport changes during layover)
     *
     * "Domestic" means both the arriving flight's destination country
     * and the departing flight's origin country are the same.
     */
    private boolean isValidConnection(FlightSegment arriving, FlightSegment departing) {
        // Airport continuity check (implicit via BFS structure, but guard anyway)
        if (!arriving.getFlight().getDestination().equals(departing.getFlight().getOrigin())) {
            return false;
        }

        long layoverMinutes = java.time.Duration.between(
                arriving.getArrivalUtc(), departing.getDepartureUtc()).toMinutes();

        if (layoverMinutes < 0)
            return false; // next flight departs before previous lands

        long minLayover = isDomesticConnection(arriving, departing)
                ? MIN_LAYOVER_DOMESTIC_MINUTES
                : MIN_LAYOVER_INTERNATIONAL_MINUTES;

        return layoverMinutes >= minLayover && layoverMinutes <= MAX_LAYOVER_MINUTES;
    }

    /**
     * A connection is domestic if both the arriving flight and the departing flight
     * are within the same country (checked at the connection point).
     *
     * Specifically: arriving flight's destination country == departing flight's
     * origin country.
     * (They must be the same airport, so same country — but we also check the full
     * journey:
     * arriving.origin.country == arriving.destination.country AND
     * departing.origin.country == departing.destination.country)
     *
     * Per the spec: "domestic if both the arriving and departing flights are within
     * the same country"
     * This means both flights must be fully within the same country.
     */
    private boolean isDomesticConnection(FlightSegment arriving, FlightSegment departing) {
        String arrOriginCountry = arriving.getOriginAirport().getCountry();
        String arrDestCountry = arriving.getDestinationAirport().getCountry();
        String depOriginCountry = departing.getOriginAirport().getCountry();
        String depDestCountry = departing.getDestinationAirport().getCountry();

        // All four airports must be in the same country
        return arrOriginCountry.equals(arrDestCountry)
                && arrDestCountry.equals(depOriginCountry)
                && depOriginCountry.equals(depDestCountry);
    }

    private void validateInputs(String origin, String destination, String date) {
        if (origin == null || destination == null || date == null) {
            throw new InvalidSearchException("origin, destination, and date are all required");
        }
        if (origin.equalsIgnoreCase(destination)) {
            throw new InvalidSearchException("Origin and destination airports must be different");
        }
        parseDate(date); // will throw if invalid
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidSearchException("Invalid date format. Use YYYY-MM-DD");
        }
    }
}
