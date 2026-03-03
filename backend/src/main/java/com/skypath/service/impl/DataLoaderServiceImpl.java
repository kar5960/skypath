package com.skypath.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypath.exception.DataLoadException;
import com.skypath.model.Airport;
import com.skypath.model.Flight;
import com.skypath.model.FlightData;
import com.skypath.service.DataLoaderService;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads flight data from the JSON file at application startup.
 * Builds in-memory indexes for efficient lookup during search.
 */
@Startup
@ApplicationScoped
public class DataLoaderServiceImpl implements DataLoaderService {

    private static final Logger LOG = Logger.getLogger(DataLoaderServiceImpl.class);

    @ConfigProperty(name = "skypath.flights.data-path", defaultValue = "/data/flights.json")
    String dataFilePath;

    @Inject
    ObjectMapper objectMapper;

    private List<Airport> airports = Collections.emptyList();
    private List<Flight> flights = Collections.emptyList();
    private Map<String, Airport> airportByCode = Collections.emptyMap();
    private Map<String, List<Flight>> flightsByOrigin = Collections.emptyMap();

    @PostConstruct
    void load() {
        LOG.infof("Loading flight data from: %s", dataFilePath);
        File file = new File(dataFilePath);

        if (!file.exists()) {
            throw new DataLoadException(
                "Flight data file not found at: " + dataFilePath, null
            );
        }

        try {
            FlightData data = objectMapper.readValue(file, FlightData.class);

            this.airports = Collections.unmodifiableList(
                data.getAirports() != null ? data.getAirports() : Collections.emptyList()
            );
            this.flights = Collections.unmodifiableList(
                data.getFlights() != null ? data.getFlights() : Collections.emptyList()
            );

            // Build airport lookup index
            this.airportByCode = airports.stream()
                    .collect(Collectors.toUnmodifiableMap(Airport::getCode, a -> a));

            // Build flights-by-origin index for fast lookup during search
            this.flightsByOrigin = flights.stream()
                    .collect(Collectors.groupingBy(
                            Flight::getOrigin,
                            Collectors.toUnmodifiableList()
                    ));

            LOG.infof("Loaded %d airports and %d flights", airports.size(), flights.size());

        } catch (DataLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new DataLoadException("Failed to parse flight data file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Airport> getAirports() {
        return airports;
    }

    @Override
    public List<Flight> getFlights() {
        return flights;
    }

    @Override
    public Optional<Airport> findAirport(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(airportByCode.get(code.toUpperCase()));
    }

    @Override
    public List<Flight> getFlightsByOrigin(String originCode) {
        return flightsByOrigin.getOrDefault(originCode.toUpperCase(), Collections.emptyList());
    }
}
