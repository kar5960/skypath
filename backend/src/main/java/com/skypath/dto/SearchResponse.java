package com.skypath.dto;

import com.skypath.model.Itinerary;
import com.skypath.model.FlightSegment;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON-serializable response for the flight search endpoint.
 */
public class SearchResponse {

    private final List<ItineraryDto> itineraries;
    private final int totalResults;

    public SearchResponse(List<Itinerary> itineraries) {
        this.itineraries = itineraries.stream()
                .map(ItineraryDto::from)
                .toList();
        this.totalResults = this.itineraries.size();
    }

    public List<ItineraryDto> getItineraries() { return itineraries; }
    public int getTotalResults() { return totalResults; }

    // -------------------------------------------------------------------------
    // Nested DTOs
    // -------------------------------------------------------------------------

    public static class ItineraryDto {
        private List<SegmentDto> segments;
        private List<LayoverDto> layovers;
        private long totalDurationMinutes;
        private String totalDurationFormatted;
        private double totalPrice;
        private int stops;

        public static ItineraryDto from(Itinerary itinerary) {
            ItineraryDto dto = new ItineraryDto();
            dto.stops = itinerary.getStopCount();
            dto.totalDurationMinutes = itinerary.getTotalDurationMinutes();
            dto.totalDurationFormatted = formatDuration(itinerary.getTotalDurationMinutes());
            dto.totalPrice = Math.round(itinerary.getTotalPrice() * 100.0) / 100.0;

            List<FlightSegment> segs = itinerary.getSegments();
            dto.segments = segs.stream().map(SegmentDto::from).toList();

            dto.layovers = new ArrayList<>();
            for (int i = 0; i < segs.size() - 1; i++) {
                dto.layovers.add(LayoverDto.from(
                        segs.get(i).getDestinationAirport().getCode(),
                        segs.get(i).getDestinationAirport().getCity(),
                        itinerary.getLayoverMinutes(i)
                ));
            }
            return dto;
        }

        private static String formatDuration(long minutes) {
            long h = minutes / 60;
            long m = minutes % 60;
            return h + "h " + m + "m";
        }

        public List<SegmentDto> getSegments() { return segments; }
        public List<LayoverDto> getLayovers() { return layovers; }
        public long getTotalDurationMinutes() { return totalDurationMinutes; }
        public String getTotalDurationFormatted() { return totalDurationFormatted; }
        public double getTotalPrice() { return totalPrice; }
        public int getStops() { return stops; }
    }

    public static class SegmentDto {

        private String flightNumber;
        private String airline;
        private String aircraft;
        private String originCode;
        private String originCity;
        private String destinationCode;
        private String destinationCity;
        private String departureTime;   // local time ISO
        private String arrivalTime;     // local time ISO
        private String departureTimezone;
        private String arrivalTimezone;
        private long durationMinutes;
        private String durationFormatted;
        private double price;

        public static SegmentDto from(FlightSegment seg) {
            SegmentDto dto = new SegmentDto();
            dto.flightNumber = seg.getFlight().getFlightNumber();
            dto.airline = seg.getFlight().getAirline();
            dto.aircraft = seg.getFlight().getAircraft();
            dto.originCode = seg.getOriginAirport().getCode();
            dto.originCity = seg.getOriginAirport().getCity();
            dto.destinationCode = seg.getDestinationAirport().getCode();
            dto.destinationCity = seg.getDestinationAirport().getCity();
            dto.departureTime = seg.getFlight().getDepartureTime();
            dto.arrivalTime = seg.getFlight().getArrivalTime();
            dto.departureTimezone = seg.getOriginAirport().getTimezone();
            dto.arrivalTimezone = seg.getDestinationAirport().getTimezone();
            dto.durationMinutes = seg.getDurationMinutes();
            dto.durationFormatted = formatDuration(seg.getDurationMinutes());
            dto.price = seg.getFlight().getPrice();
            return dto;
        }

        private static String formatDuration(long minutes) {
            long h = minutes / 60;
            long m = minutes % 60;
            return h + "h " + m + "m";
        }

        public String getFlightNumber() { return flightNumber; }
        public String getAirline() { return airline; }
        public String getAircraft() { return aircraft; }
        public String getOriginCode() { return originCode; }
        public String getOriginCity() { return originCity; }
        public String getDestinationCode() { return destinationCode; }
        public String getDestinationCity() { return destinationCity; }
        public String getDepartureTime() { return departureTime; }
        public String getArrivalTime() { return arrivalTime; }
        public String getDepartureTimezone() { return departureTimezone; }
        public String getArrivalTimezone() { return arrivalTimezone; }
        public long getDurationMinutes() { return durationMinutes; }
        public String getDurationFormatted() { return durationFormatted; }
        public double getPrice() { return price; }
    }

    public static class LayoverDto {
        private String airportCode;
        private String airportCity;
        private long durationMinutes;
        private String durationFormatted;

        public static LayoverDto from(String code, String city, long minutes) {
            LayoverDto dto = new LayoverDto();
            dto.airportCode = code;
            dto.airportCity = city;
            dto.durationMinutes = minutes;
            dto.durationFormatted = formatDuration(minutes);
            return dto;
        }

        private static String formatDuration(long minutes) {
            long h = minutes / 60;
            long m = minutes % 60;
            return h + "h " + m + "m";
        }

        public String getAirportCode() { return airportCode; }
        public String getAirportCity() { return airportCity; }
        public long getDurationMinutes() { return durationMinutes; }
        public String getDurationFormatted() { return durationFormatted; }
    }
}
