package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.weather.WeatherResponse;

public record CondicionesMeteorologicas(
        WeatherResponse respuesta,
        int codigoCondicion,
        String grupoCondicion
) {
}
