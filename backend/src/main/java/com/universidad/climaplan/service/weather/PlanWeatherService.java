package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.weather.PlanWeatherCheckResponse;
import com.universidad.climaplan.dto.weather.WeatherDataType;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.service.PlanService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PlanWeatherService {

    private final PlanService planService;
    private final OpenWeatherService openWeatherService;

    public PlanWeatherService(PlanService planService, OpenWeatherService openWeatherService) {
        this.planService = planService;
        this.openWeatherService = openWeatherService;
    }

    public PlanWeatherCheckResponse verificarPlan(Long planId, String email, boolean administrador) {
        PlanResponse plan = planService.obtenerPorId(planId, email, administrador);
        return evaluarPlan(plan);
    }

    public PlanWeatherCheckResponse verificarPlanCompartido(Plan plan) {
        return evaluarPlan(new PlanResponse(
                plan.getId(),
                plan.getTitulo(),
                plan.getCiudad(),
                plan.getFecha(),
                plan.getTipoActividad(),
                null,
                plan.getEstado(),
                null,
                null
        ));
    }

    private PlanWeatherCheckResponse evaluarPlan(PlanResponse plan) {
        LocalDate hoy = LocalDate.now();
        if (plan.fecha().isBefore(hoy)) {
            return respuestaSinClima(plan, WeatherDataType.PAST_DATE,
                    "La fecha de este plan ya pasó.");
        }
        if (plan.fecha().isEqual(hoy)) {
            return respuestaConClima(
                    plan,
                    WeatherDataType.CURRENT,
                    openWeatherService.consultarClimaDetallado(plan.ciudad())
            );
        }

        Optional<CondicionesMeteorologicas> pronostico =
                openWeatherService.consultarPronostico(plan.ciudad(), plan.fecha());
        return pronostico
                .map(clima -> respuestaConClima(plan, WeatherDataType.FORECAST, clima))
                .orElseGet(() -> respuestaSinClima(
                        plan,
                        WeatherDataType.NOT_AVAILABLE,
                        "El pronóstico todavía no está disponible para esta fecha. "
                                + "Intenta nuevamente cuando falten 5 días o menos."
                ));
    }

    EstadoClimatico evaluar(CondicionesMeteorologicas clima) {
        WeatherResponse weather = clima.respuesta();

        boolean lluviaFuerte = clima.codigoCondicion() >= 502 && clima.codigoCondicion() <= 504
                || clima.codigoCondicion() == 511;
        boolean tormenta = clima.codigoCondicion() >= 200 && clima.codigoCondicion() < 300;
        boolean nieve = clima.codigoCondicion() >= 600 && clima.codigoCondicion() < 700;

        if (weather.temperatura() < 10 || weather.temperatura() > 36
                || weather.velocidadViento() >= 12 || lluviaFuerte || tormenta || nieve) {
            return EstadoClimatico.NO_RECOMENDABLE;
        }

        boolean condicionesModeradas = clima.codigoCondicion() >= 300
                && clima.codigoCondicion() < 502;
        boolean temperaturaPrecaucion = weather.temperatura() < 18 || weather.temperatura() > 32;
        boolean humedadAlta = weather.humedad() >= 80;
        boolean vientoModerado = weather.velocidadViento() >= 8;

        if (condicionesModeradas || temperaturaPrecaucion || humedadAlta || vientoModerado) {
            return EstadoClimatico.PRECAUCION;
        }
        return EstadoClimatico.FAVORABLE;
    }

    private PlanWeatherCheckResponse respuestaConClima(
            PlanResponse plan,
            WeatherDataType tipo,
            CondicionesMeteorologicas condiciones
    ) {
        EstadoClimatico estado = evaluar(condiciones);
        return new PlanWeatherCheckResponse(
                plan.id(),
                plan.titulo(),
                plan.ciudad(),
                plan.fecha(),
                tipo,
                estado,
                condiciones.respuesta(),
                recomendacion(estado, tipo),
                tipo == WeatherDataType.CURRENT
                        ? "Clima actual"
                        : "Pronóstico para " + plan.fecha()
        );
    }

    private PlanWeatherCheckResponse respuestaSinClima(
            PlanResponse plan,
            WeatherDataType tipo,
            String mensaje
    ) {
        return new PlanWeatherCheckResponse(
                plan.id(),
                plan.titulo(),
                plan.ciudad(),
                plan.fecha(),
                tipo,
                null,
                null,
                null,
                mensaje
        );
    }

    private String recomendacion(EstadoClimatico estado, WeatherDataType tipo) {
        boolean esPronostico = tipo == WeatherDataType.FORECAST;
        return switch (estado) {
            case FAVORABLE -> esPronostico
                    ? "Las condiciones pronosticadas son favorables para la actividad."
                    : "Las condiciones actuales son favorables para la actividad.";
            case PRECAUCION -> esPronostico
                    ? "La actividad es posible, pero considera las condiciones pronosticadas."
                    : "La actividad es posible, pero considera las condiciones del clima.";
            case NO_RECOMENDABLE -> esPronostico
                    ? "Se recomienda reprogramar la actividad por las condiciones pronosticadas."
                    : "Se recomienda reprogramar la actividad por las condiciones actuales.";
        };
    }
}
