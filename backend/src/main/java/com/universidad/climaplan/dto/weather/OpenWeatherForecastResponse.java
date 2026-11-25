package com.universidad.climaplan.dto.weather;

import java.util.List;

public record OpenWeatherForecastResponse(
        City city,
        List<ForecastItem> list
) {

    public record City(Integer timezone) {
    }

    public record ForecastItem(
            long dt,
            OpenWeatherResponse.MainData main,
            List<OpenWeatherResponse.WeatherCondition> weather,
            OpenWeatherResponse.WindData wind
    ) {
    }
}
