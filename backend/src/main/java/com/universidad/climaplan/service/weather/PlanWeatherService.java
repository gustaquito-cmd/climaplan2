package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.PlanResponse;
import com.universidad.climaplan.dto.weather.EstadoClimatico;
import com.universidad.climaplan.dto.weather.PlanWeatherCheckResponse;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.entity.Plan;
import com.universidad.climaplan.service.PlanService;
import org.springframework.stereotype.Service;

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
        ClimaActual climaActual = openWeatherService.consultarClimaDetallado(plan.ciudad());
        WeatherResponse clima = climaActual.respuesta();
        EstadoClimatico estado = evaluar(climaActual);

        return new PlanWeatherCheckResponse(
                plan.id(),
                plan.titulo(),
                plan.ciudad(),
                plan.fecha(),
                estado,
                clima,
                recomendacion(estado)
        );
    }

    EstadoClimatico evaluar(ClimaActual clima) {
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

    private String recomendacion(EstadoClimatico estado) {
        return switch (estado) {
            case FAVORABLE -> "Las condiciones actuales son favorables para la actividad.";
            case PRECAUCION -> "La actividad es posible, pero considera las condiciones del clima.";
            case NO_RECOMENDABLE -> "Se recomienda reprogramar la actividad por las condiciones actuales.";
        };
    }
}
