package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.service.PlanService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class PlanWeatherServiceTest {

    private final PlanWeatherService service =
            new PlanWeatherService(mock(PlanService.class), mock(OpenWeatherService.class));

    @Test
    void condicionesIdealesSonFavorables() {
        assertEquals(EstadoClimatico.FAVORABLE, evaluar(25, 50, 3, 800, "Clear"));
    }

    @Test
    void humedadAltaOlluviaLigeraRequierenPrecaucion() {
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(25, 85, 3, 800, "Clear"));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(25, 50, 3, 500, "Rain"));
    }

    @Test
    void lluviaFuerteTemperaturaExtremaOVientoFuerteNoSonRecomendables() {
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(25, 50, 3, 502, "Rain"));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(9.9, 50, 3, 800, "Clear"));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(25, 50, 12, 800, "Clear"));
    }

    @Test
    void aplicaLosLimitesDeTemperaturaYClasificaPorCodigoMeteorologico() {
        assertEquals(EstadoClimatico.FAVORABLE, evaluar(18, 50, 3, 800, "No importa"));
        assertEquals(EstadoClimatico.FAVORABLE, evaluar(32, 50, 3, 800, "No importa"));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(10, 50, 3, 800, "Clear"));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(36, 50, 3, 800, "Clear"));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(36.1, 50, 3, 800, "Clear"));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(25, 50, 3, 202, "No importa"));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(25, 50, 3, 601, "No importa"));
    }

    @Test
    void respuestaDePlanIncluyeElClimaActualAnidado() {
        PlanService planService = mock(PlanService.class);
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        PlanWeatherService planWeatherService = new PlanWeatherService(planService, openWeatherService);
        PlanResponse plan = new PlanResponse(
                7L, "Caminata", "Tuxtla Gutiérrez", LocalDate.of(2026, 11, 8),
                "EXTERIOR", null, null, 2L, "Usuario"
        );
        WeatherResponse weather = new WeatherResponse(
                "Tuxtla Gutiérrez", "MX", 16.75, -93.12, 25, 25, 50,
                "cielo claro", 3
        );
        when(planService.obtenerPorId(7L, "usuario@example.com", false)).thenReturn(plan);
        when(openWeatherService.consultarClimaDetallado(plan.ciudad()))
                .thenReturn(new ClimaActual(weather, 800, "Clear"));

        var response = planWeatherService.verificarPlan(7L, "usuario@example.com", false);

        assertEquals(7L, response.planId());
        assertEquals("Caminata", response.titulo());
        assertEquals(plan.fecha(), response.fecha());
        assertEquals(weather, response.clima());
        assertEquals(EstadoClimatico.FAVORABLE, response.estadoClimatico());
    }

    private EstadoClimatico evaluar(
            double temperatura,
            int humedad,
            double viento,
            int codigo,
            String grupo
    ) {
        WeatherResponse response = new WeatherResponse(
                "Tuxtla Gutiérrez",
                "MX",
                16.75,
                -93.12,
                temperatura,
                temperatura,
                humedad,
                "despejado",
                viento
        );
        return service.evaluar(new ClimaActual(response, codigo, grupo));
    }
}
