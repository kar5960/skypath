package com.skypath.service;

import com.skypath.model.Itinerary;

import java.util.List;

/**
 * Contract for searching itineraries between two airports on a given date.
 */
public interface FlightSearchService {

    /**
     * Find all valid itineraries (direct + up to 2 stops) from origin to destination on date.
     *
     * @param origin      IATA code of departure airport
     * @param destination IATA code of arrival airport
     * @param date        ISO date (YYYY-MM-DD)
     * @return list of valid itineraries, sorted by total travel time ascending
     */
    List<Itinerary> search(String origin, String destination, String date);
}
