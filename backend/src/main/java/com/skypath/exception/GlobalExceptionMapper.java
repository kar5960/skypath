package com.skypath.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 * All error responses share the same JSON envelope: { "error": "...", "message": "..." }
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof InvalidAirportException ex) {
            return errorResponse(Response.Status.BAD_REQUEST, "INVALID_AIRPORT", ex.getMessage());
        }

        if (exception instanceof InvalidSearchException ex) {
            return errorResponse(Response.Status.BAD_REQUEST, "INVALID_SEARCH", ex.getMessage());
        }

        if (exception instanceof ConstraintViolationException ex) {
            String violations = ex.getConstraintViolations().stream()
                    .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                    .collect(Collectors.joining("; "));
            return errorResponse(Response.Status.BAD_REQUEST, "VALIDATION_ERROR", violations);
        }

        if (exception instanceof DataLoadException ex) {
            LOG.error("Data load failure", ex);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "DATA_LOAD_ERROR",
                    "Flight data could not be loaded. Please check the data file.");
        }

        LOG.error("Unexpected error", exception);
        return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again.");
    }

    private Response errorResponse(Response.Status status, String error, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", error, "message", message))
                .build();
    }
}
