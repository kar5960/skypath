package com.skypath.model;

import java.time.ZonedDateTime;

/**
 * Represents a single flight leg within an itinerary.
 * Holds both the raw Flight data and resolved ZonedDateTime for computation.
 */
public class FlightSegment {

    private final Flight flight;
    private final Airport originAirport;
    private final Airport destinationAirport;
    private final ZonedDateTime departureUtc;
    private final ZonedDateTime arrivalUtc;

    public FlightSegment(Flight flight,
                         Airport originAirport,
                         Airport destinationAirport,
                         ZonedDateTime departureUtc,
                         ZonedDateTime arrivalUtc) {
        this.flight = flight;
        this.originAirport = originAirport;
        this.destinationAirport = destinationAirport;
        this.departureUtc = departureUtc;
        this.arrivalUtc = arrivalUtc;
    }

    public Flight getFlight() { return flight; }
    public Airport getOriginAirport() { return originAirport; }
    public Airport getDestinationAirport() { return destinationAirport; }
    public ZonedDateTime getDepartureUtc() { return departureUtc; }
    public ZonedDateTime getArrivalUtc() { return arrivalUtc; }

    /** Flight duration in minutes */
    public long getDurationMinutes() {
        return java.time.Duration.between(departureUtc, arrivalUtc).toMinutes();
    }
}
