package com.skypath.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Represents a single flight from the dataset.
 *
 * Data quirks handled here:
 * - price is mixed-type in source JSON: some entries are numeric (299.0, 1),
 *   others are strings ("289.00", "99"). FlexibleDoubleDeserializer handles all forms.
 * - SP995 has a typo origin "JKF" — it will fail airport lookup in the service
 *   and be skipped with a WARN log (graceful degradation).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Flight {

    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private double price;
    private String aircraft;

    public Flight() {}

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public double getPrice() { return price; }

    @JsonDeserialize(using = FlexibleDoubleDeserializer.class)
    public void setPrice(double price) { this.price = price; }

    public String getAircraft() { return aircraft; }
    public void setAircraft(String aircraft) { this.aircraft = aircraft; }

    @Override
    public String toString() {
        return String.format("Flight{%s, %s->%s, dep=%s, arr=%s, price=%.2f}",
                flightNumber, origin, destination, departureTime, arrivalTime, price);
    }

    // -------------------------------------------------------------------------
    // Inner deserializer: handles price as number OR quoted string
    // -------------------------------------------------------------------------

    public static class FlexibleDoubleDeserializer extends StdDeserializer<Double> {

        public FlexibleDoubleDeserializer() {
            super(Double.class);
        }

        @Override
        public Double deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                String text = p.getText().trim();
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    throw ctx.weirdStringException(text, Double.class, "Cannot parse price: " + text);
                }
            }
            return p.getDoubleValue();
        }
    }
}
