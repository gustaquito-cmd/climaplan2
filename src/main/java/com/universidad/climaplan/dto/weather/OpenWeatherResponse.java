package com.universidad.climaplan.dto.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenWeatherResponse(
        String name,
        MainData main,
        List<WeatherCondition> weather,
        WindData wind
) {

    public record MainData(
            double temp,
            @JsonProperty("feels_like") double feelsLike,
            int humidity
    ) {
    }

    public record WeatherCondition(
            int id,
            String main,
            String description
    ) {
    }

    public record WindData(double speed) {
    }
}
