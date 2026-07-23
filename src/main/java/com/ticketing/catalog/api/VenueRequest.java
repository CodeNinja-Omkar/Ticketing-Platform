package com.ticketing.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record VenueRequest (
    @NotBlank(message = "Name is required")
    String name,

    @NotBlank(message = "City is required")
    String city,

    @Min(value=1 ,message = "Capacity must alteast be 1")
    int capacity

){

}
