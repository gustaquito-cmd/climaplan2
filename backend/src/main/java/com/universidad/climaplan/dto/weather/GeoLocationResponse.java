package com.universidad.climaplan.dto.weather;

public record GeoLocationResponse(
        String name,
        double lat,
        double lon,
        String country
) {
}
