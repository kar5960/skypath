package com.skypath.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SearchRequest {

    @NotBlank(message = "Origin airport code is required")
    @Size(min = 3, max = 3, message = "Airport code must be exactly 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Airport code must be 3 uppercase letters")
    private String origin;

    @NotBlank(message = "Destination airport code is required")
    @Size(min = 3, max = 3, message = "Airport code must be exactly 3 characters")
    @Pattern(regexp = "[A-Z]{3}", message = "Airport code must be 3 uppercase letters")
    private String destination;

    @NotBlank(message = "Date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in YYYY-MM-DD format")
    private String date;

    public SearchRequest() {}

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin != null ? origin.toUpperCase() : null; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination != null ? destination.toUpperCase() : null; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
