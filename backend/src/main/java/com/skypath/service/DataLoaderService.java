package com.skypath.service;

import com.skypath.model.Airport;
import com.skypath.model.Flight;

import java.util.List;
import java.util.Optional;

/**
 * Contract for loading and accessing the flight dataset.
 */
public interface DataLoaderService {

    /** All airports in the dataset */
    List<Airport> getAirports();

    /** All flights in the dataset */
    List<Flight> getFlights();

    /** Lookup an airport by its IATA code */
    Optional<Airport> findAirport(String code);

    /** All flights departing from a given airport */
    List<Flight> getFlightsByOrigin(String originCode);
}
