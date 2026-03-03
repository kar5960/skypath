package com.skypath.controller;

import com.skypath.dto.SearchRequest;
import com.skypath.dto.SearchResponse;
import com.skypath.model.Itinerary;
import com.skypath.service.FlightSearchService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/api/flights")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Flight Search", description = "Search for direct and connecting flight itineraries")
public class FlightSearchResource {

    private static final Logger LOG = Logger.getLogger(FlightSearchResource.class);

    @Inject
    FlightSearchService flightSearchService;

    @GET
    @Path("/search")
    @Operation(
        summary = "Search flights (GET)",
        description = "Find all valid itineraries between two airports on a given date. " +
                      "Returns direct flights and itineraries with up to 2 stops, sorted by total travel time."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Search results (may be empty list)"),
        @APIResponse(responseCode = "400", description = "Invalid airport code or date format")
    })
    public Response searchGet(
            @Parameter(description = "IATA origin airport code", example = "JFK", required = true)
            @QueryParam("origin") String origin,

            @Parameter(description = "IATA destination airport code", example = "LAX", required = true)
            @QueryParam("destination") String destination,

            @Parameter(description = "Departure date in YYYY-MM-DD format", example = "2024-03-15", required = true)
            @QueryParam("date") String date) {

        LOG.infof("GET /api/flights/search origin=%s destination=%s date=%s", origin, destination, date);
        return executeSearch(origin, destination, date);
    }

    @POST
    @Path("/search")
    @Operation(
        summary = "Search flights (POST)",
        description = "Same as GET /search but accepts a JSON body. Useful when building integrations."
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Search results (may be empty list)"),
        @APIResponse(responseCode = "400", description = "Validation error or invalid airport code")
    })
    public Response searchPost(@Valid SearchRequest request) {
        LOG.infof("POST /api/flights/search origin=%s destination=%s date=%s",
                request.getOrigin(), request.getDestination(), request.getDate());
        return executeSearch(request.getOrigin(), request.getDestination(), request.getDate());
    }

    @GET
    @Path("/health")
    @Operation(summary = "Health check", description = "Returns UP if the service is running and data is loaded.")
    @APIResponse(responseCode = "200", description = "Service is healthy")
    public Response health() {
        return Response.ok(Map.of("status", "UP")).build();
    }

    // -------------------------------------------------------------------------

    private Response executeSearch(String origin, String destination, String date) {
        List<Itinerary> itineraries = flightSearchService.search(origin, destination, date);
        return Response.ok(new SearchResponse(itineraries)).build();
    }
}
