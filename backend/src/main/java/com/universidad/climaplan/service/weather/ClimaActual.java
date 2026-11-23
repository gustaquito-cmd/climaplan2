package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.weather.WeatherResponse;

public record ClimaActual(
        WeatherResponse respuesta,
        int codigoCondicion,
        String grupoCondicion
) {
}
