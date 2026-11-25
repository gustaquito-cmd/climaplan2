package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.weather.WeatherDataType;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.entity.EstadoPlan;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.service.PlanService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanWeatherServiceTest {

    @Test
    void planParaHoyConsultaClimaActual() {
        PlanService planService = mock(PlanService.class);
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        PlanWeatherService service = new PlanWeatherService(planService, openWeatherService);
        PlanResponse plan = plan(LocalDate.now());
        WeatherResponse weather = weather(25, 50, 3, "cielo claro");
        when(planService.obtenerPorId(7L, "usuario@example.com", false)).thenReturn(plan);
        when(openWeatherService.consultarClimaDetallado(plan.ciudad()))
                .thenReturn(new CondicionesMeteorologicas(weather, 800, "Clear"));

        var response = service.verificarPlan(7L, "usuario@example.com", false);

        assertEquals(WeatherDataType.CURRENT, response.weatherDataType());
        assertEquals(weather, response.clima());
        assertEquals(EstadoClimatico.FAVORABLE, response.estadoClimatico());
        assertEquals("Clima actual", response.mensaje());
        verify(openWeatherService).consultarClimaDetallado(plan.ciudad());
        verify(openWeatherService, never()).consultarPronostico(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void planFuturoUsaPronosticoYConservaEvaluacion() {
        PlanService planService = mock(PlanService.class);
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        PlanWeatherService service = new PlanWeatherService(planService, openWeatherService);
        LocalDate fecha = LocalDate.now().plusDays(3);
        PlanResponse plan = plan(fecha);
        WeatherResponse weather = weather(25, 50, 3, "cielo despejado");
        when(planService.obtenerPorId(7L, "usuario@example.com", false)).thenReturn(plan);
        when(openWeatherService.consultarPronostico(plan.ciudad(), fecha))
                .thenReturn(Optional.of(new CondicionesMeteorologicas(weather, 800, "Clear")));

        var response = service.verificarPlan(7L, "usuario@example.com", false);

        assertEquals(WeatherDataType.FORECAST, response.weatherDataType());
        assertEquals(weather, response.clima());
        assertEquals(EstadoClimatico.FAVORABLE, response.estadoClimatico());
        assertEquals("Las condiciones pronosticadas son favorables para la actividad.",
                response.recomendacion());
        assertEquals("Pronóstico para " + fecha, response.mensaje());
        verify(openWeatherService, never()).consultarClimaDetallado(plan.ciudad());
    }

    @Test
    void futuroSinEntradaParaLaFechaNoFingeClimaActual() {
        PlanService planService = mock(PlanService.class);
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        PlanWeatherService service = new PlanWeatherService(planService, openWeatherService);
        LocalDate fecha = LocalDate.now().plusDays(12);
        PlanResponse plan = plan(fecha);
        when(planService.obtenerPorId(7L, "usuario@example.com", false)).thenReturn(plan);
        when(openWeatherService.consultarPronostico(plan.ciudad(), fecha)).thenReturn(Optional.empty());

        var response = service.verificarPlan(7L, "usuario@example.com", false);

        assertEquals(WeatherDataType.NOT_AVAILABLE, response.weatherDataType());
        assertNull(response.clima());
        assertNull(response.estadoClimatico());
        assertEquals(
                "El pronóstico todavía no está disponible para esta fecha. "
                        + "Intenta nuevamente cuando falten 5 días o menos.",
                response.mensaje()
        );
        verify(openWeatherService, never()).consultarClimaDetallado(plan.ciudad());
    }

    @Test
    void planPasadoNoConsultaOpenWeather() {
        PlanService planService = mock(PlanService.class);
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        PlanWeatherService service = new PlanWeatherService(planService, openWeatherService);
        PlanResponse plan = plan(LocalDate.now().minusDays(1));
        when(planService.obtenerPorId(7L, "usuario@example.com", false)).thenReturn(plan);

        var response = service.verificarPlan(7L, "usuario@example.com", false);

        assertEquals(WeatherDataType.PAST_DATE, response.weatherDataType());
        assertNull(response.clima());
        assertEquals("La fecha de este plan ya pasó.", response.mensaje());
        verify(openWeatherService, never()).consultarClimaDetallado(plan.ciudad());
        verify(openWeatherService, never()).consultarPronostico(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void planCompartidoUsaLaMismaDecisionDePronostico() {
        OpenWeatherService openWeatherService = mock(OpenWeatherService.class);
        PlanWeatherService service = new PlanWeatherService(mock(PlanService.class), openWeatherService);
        Plan plan = new Plan();
        plan.setTitulo("Caminata");
        plan.setCiudad("Tuxtla Gutiérrez");
        plan.setFecha(LocalDate.now().plusDays(3));
        plan.setTipoActividad("Senderismo");
        plan.setEstado(EstadoPlan.PENDIENTE);
        when(openWeatherService.consultarPronostico(plan.getCiudad(), plan.getFecha()))
                .thenReturn(Optional.of(new CondicionesMeteorologicas(
                        weather(25, 50, 3, "cielo claro"),
                        800,
                        "Clear"
                )));

        var response = service.verificarPlanCompartido(plan);

        assertEquals(WeatherDataType.FORECAST, response.weatherDataType());
        assertEquals("cielo claro", response.clima().descripcion());
        verify(openWeatherService, never()).consultarClimaDetallado(plan.getCiudad());
    }

    @Test
    void aplicaLosLimitesDeTemperaturaYClasificaPorCodigoMeteorologico() {
        PlanWeatherService service = new PlanWeatherService(
                mock(PlanService.class),
                mock(OpenWeatherService.class)
        );

        assertEquals(EstadoClimatico.FAVORABLE, evaluar(service, 18, 50, 3, 800));
        assertEquals(EstadoClimatico.FAVORABLE, evaluar(service, 32, 50, 3, 800));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(service, 10, 50, 3, 800));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(service, 36, 50, 3, 800));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(service, 25, 85, 3, 800));
        assertEquals(EstadoClimatico.PRECAUCION, evaluar(service, 25, 50, 3, 500));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(service, 9.9, 50, 3, 800));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(service, 36.1, 50, 3, 800));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(service, 25, 50, 12, 800));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(service, 25, 50, 3, 202));
        assertEquals(EstadoClimatico.NO_RECOMENDABLE, evaluar(service, 25, 50, 3, 601));
    }

    private static PlanResponse plan(LocalDate fecha) {
        return new PlanResponse(
                7L,
                "Caminata",
                "Tuxtla Gutiérrez",
                fecha,
                "EXTERIOR",
                null,
                EstadoPlan.PENDIENTE,
                2L,
                "Usuario"
        );
    }

    private static WeatherResponse weather(
            double temperatura,
            int humedad,
            double viento,
            String descripcion
    ) {
        return new WeatherResponse(
                "Tuxtla Gutiérrez",
                "MX",
                16.75,
                -93.12,
                temperatura,
                temperatura,
                humedad,
                descripcion,
                viento
        );
    }

    private static EstadoClimatico evaluar(
            PlanWeatherService service,
            double temperatura,
            int humedad,
            double viento,
            int codigo
    ) {
        return service.evaluar(new CondicionesMeteorologicas(
                weather(temperatura, humedad, viento, "despejado"),
                codigo,
                "Clear"
        ));
    }
}
