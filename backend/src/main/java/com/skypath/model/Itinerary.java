package com.skypath.model;

import java.time.Duration;
import java.util.List;

/**
 * A complete itinerary: one or more flight segments forming a trip from origin to destination.
 */
public class Itinerary {

    private final List<FlightSegment> segments;
    private final long totalDurationMinutes;
    private final double totalPrice;

    public Itinerary(List<FlightSegment> segments) {
        this.segments = segments;
        this.totalPrice = segments.stream()
                .mapToDouble(s -> s.getFlight().getPrice())
                .sum();

        // Total duration: from first departure to last arrival (UTC)
        this.totalDurationMinutes = Duration.between(
                segments.get(0).getDepartureUtc(),
                segments.get(segments.size() - 1).getArrivalUtc()
        ).toMinutes();
    }

    public List<FlightSegment> getSegments() { return segments; }
    public long getTotalDurationMinutes() { return totalDurationMinutes; }
    public double getTotalPrice() { return totalPrice; }

    /** Layover duration in minutes between segment[i] arrival and segment[i+1] departure */
    public long getLayoverMinutes(int afterSegmentIndex) {
        if (afterSegmentIndex >= segments.size() - 1) {
            throw new IndexOutOfBoundsException("No layover after last segment");
        }
        FlightSegment arriving = segments.get(afterSegmentIndex);
        FlightSegment departing = segments.get(afterSegmentIndex + 1);
        return Duration.between(arriving.getArrivalUtc(), departing.getDepartureUtc()).toMinutes();
    }

    public int getStopCount() {
        return segments.size() - 1;
    }
}
