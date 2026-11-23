package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.dto.weather.GeoLocationResponse;
import com.universidad.climaplan.dto.weather.OpenWeatherResponse;
import com.universidad.climaplan.dto.weather.WeatherResponse;
import com.universidad.climaplan.exception.OpenWeatherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class OpenWeatherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenWeatherService.class);
    private static final ParameterizedTypeReference<List<GeoLocationResponse>> GEOLOCATION_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String apiKey;
    private final String geoBaseUrl;
    private final String weatherBaseUrl;

    public OpenWeatherService(
            RestClient.Builder restClientBuilder,
            @Value("${openweather.api-key:}") String apiKey,
            @Value("${openweather.geo-base-url}") String geoBaseUrl,
            @Value("${openweather.weather-base-url}") String weatherBaseUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.geoBaseUrl = geoBaseUrl;
        this.weatherBaseUrl = weatherBaseUrl;
    }

    public WeatherResponse consultarClima(String ciudad) {
        return consultarClimaDetallado(ciudad).respuesta();
    }

    public ClimaActual consultarClimaDetallado(String ciudad) {
        validarConfiguracion();
        if (ciudad == null || ciudad.isBlank()) {
            throw new OpenWeatherException(HttpStatus.BAD_REQUEST, "El parámetro city es obligatorio");
        }

        GeoLocationResponse ubicacion = obtenerUbicacion(ciudad.trim());
        OpenWeatherResponse clima = obtenerClimaActual(ubicacion);
        validarRespuestaClima(clima);
        WeatherResponse respuesta = new WeatherResponse(
                ubicacion.name(),
                ubicacion.country(),
                ubicacion.lat(),
                ubicacion.lon(),
                clima.main().temp(),
                clima.main().feelsLike(),
                clima.main().humidity(),
                clima.weather().getFirst().description(),
                clima.wind().speed()
        );
        OpenWeatherResponse.WeatherCondition condicion = clima.weather().getFirst();
        return new ClimaActual(respuesta, condicion.id(), condicion.main());
    }

    private GeoLocationResponse obtenerUbicacion(String ciudad) {
        try {
            List<GeoLocationResponse> resultados = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(geoBaseUrl)
                            .path("/direct")
                            .queryParam("q", "{city},MX")
                            .queryParam("limit", 1)
                            .queryParam("appid", "{apiKey}")
                            .encode()
                            .buildAndExpand(Map.of("city", ciudad, "apiKey", apiKey))
                            .toUri())
                    .retrieve()
                    .body(GEOLOCATION_TYPE);
            if (resultados == null || resultados.isEmpty()) {
                throw new OpenWeatherException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la ciudad: " + ciudad
                );
            }
            return resultados.getFirst();
        } catch (OpenWeatherException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw convertirErrorProveedor(exception);
        } catch (RestClientException exception) {
            throw new OpenWeatherException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar el servicio de geocodificación",
                    exception
            );
        }
    }

    private OpenWeatherResponse obtenerClimaActual(GeoLocationResponse ubicacion) {
        try {
            OpenWeatherResponse respuesta = restClient.get()
                    .uri(UriComponentsBuilder.fromUriString(weatherBaseUrl)
                            .path("/weather")
                            .queryParam("lat", ubicacion.lat())
                            .queryParam("lon", ubicacion.lon())
                            .queryParam("appid", "{apiKey}")
                            .queryParam("units", "metric")
                            .queryParam("lang", "es")
                            .encode()
                            .buildAndExpand(Map.of("apiKey", apiKey))
                            .toUri())
                    .retrieve()
                    .body(OpenWeatherResponse.class);
            if (respuesta == null) {
                throw new OpenWeatherException(
                        HttpStatus.BAD_GATEWAY,
                        "El servicio meteorológico devolvió una respuesta vacía"
                );
            }
            return respuesta;
        } catch (OpenWeatherException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw convertirErrorProveedor(exception);
        } catch (RestClientException exception) {
            throw new OpenWeatherException(
                    HttpStatus.BAD_GATEWAY,
                    "No fue posible consultar el servicio meteorológico",
                    exception
            );
        }
    }

    private void validarConfiguracion() {
        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.error("OpenWeather no está configurado: falta la variable OPENWEATHER_API_KEY");
            throw new OpenWeatherException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "El servicio meteorológico no está configurado"
            );
        }
    }

    private void validarRespuestaClima(OpenWeatherResponse clima) {
        if (clima.main() == null || clima.wind() == null
                || clima.weather() == null || clima.weather().isEmpty()) {
            throw new OpenWeatherException(
                    HttpStatus.BAD_GATEWAY,
                    "El servicio meteorológico devolvió datos incompletos"
            );
        }
    }

    private OpenWeatherException convertirErrorProveedor(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 401) {
            return new OpenWeatherException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La clave de OpenWeather no es válida"
            );
        }
        if (exception.getStatusCode().value() == 404) {
            return new OpenWeatherException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró la ciudad consultada"
            );
        }
        return new OpenWeatherException(
                HttpStatus.BAD_GATEWAY,
                "El servicio meteorológico no pudo completar la consulta",
                exception
        );
    }
}
