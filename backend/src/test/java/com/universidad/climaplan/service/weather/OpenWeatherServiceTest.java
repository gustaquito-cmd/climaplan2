package com.universidad.climaplan.service.weather;

import com.universidad.climaplan.exception.OpenWeatherException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.hamcrest.Matchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenWeatherServiceTest {

    @Test
    void geocodificaCiudadYMapeaClimaActual() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherService service = new OpenWeatherService(
                builder,
                "api-key",
                "https://api.openweathermap.org/geo/1.0",
                "https://api.openweathermap.org/data/2.5"
        );

        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/geo/1.0/direct?"
                )))
                .andExpect(queryParam("q", "Tuxtla%20Gutierrez,MX"))
                .andExpect(queryParam("limit", "1"))
                .andExpect(queryParam("appid", "api-key"))
                .andRespond(withSuccess(
                        "[{\"name\":\"Tuxtla Gutiérrez\",\"lat\":16.75,\"lon\":-93.12,\"country\":\"MX\"}]",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/data/2.5/weather?"
                )))
                .andExpect(queryParam("lat", "16.75"))
                .andExpect(queryParam("lon", "-93.12"))
                .andExpect(queryParam("appid", "api-key"))
                .andExpect(queryParam("units", "metric"))
                .andExpect(queryParam("lang", "es"))
                .andRespond(withSuccess(
                        """
                        {
                          "name": "Tuxtla Gutiérrez",
                          "main": {"temp": 24.2, "feels_like": 25.0, "humidity": 61},
                          "weather": [{"id": 800, "main": "Clear", "description": "cielo claro"}],
                          "wind": {"speed": 2.5}
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var weather = service.consultarClima("Tuxtla Gutierrez");

        assertEquals("Tuxtla Gutiérrez", weather.ciudad());
        assertEquals("MX", weather.pais());
        assertEquals(16.75, weather.lat());
        assertEquals(-93.12, weather.lon());
        assertEquals(24.2, weather.temperatura());
        assertEquals(25.0, weather.sensacionTermica());
        assertEquals(61, weather.humedad());
        assertEquals("cielo claro", weather.descripcion());
        assertEquals(2.5, weather.velocidadViento());
        server.verify();
    }

    @Test
    void ciudadNoEncontradaDevuelve404() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherService service = new OpenWeatherService(
                builder,
                "api-key",
                "https://api.openweathermap.org/geo/1.0",
                "https://api.openweathermap.org/data/2.5"
        );
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/geo/1.0/direct?"
                )))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        OpenWeatherException exception = assertThrows(
                OpenWeatherException.class,
                () -> service.consultarClima("Ciudad Inexistente")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        server.verify();
    }

    @Test
    void forecastSeleccionaLaEntradaMasCercanaAlMediodiaEnLaFechaLocal() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherService service = new OpenWeatherService(
                builder,
                "api-key",
                "https://api.openweathermap.org/geo/1.0",
                "https://api.openweathermap.org/data/2.5"
        );
        int zonaHoraria = 3600;
        LocalDate fecha = LocalDate.of(2026, 1, 15);
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/geo/1.0/direct?"
                )))
                .andRespond(withSuccess(
                        "[{\"name\":\"Tuxtla Gutiérrez\",\"lat\":16.75,\"lon\":-93.12,\"country\":\"MX\"}]",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/data/2.5/forecast?"
                )))
                .andExpect(queryParam("lat", "16.75"))
                .andExpect(queryParam("lon", "-93.12"))
                .andExpect(queryParam("appid", "api-key"))
                .andExpect(queryParam("units", "metric"))
                .andExpect(queryParam("lang", "es"))
                .andRespond(withSuccess(
                        """
                        {
                          "city": {"timezone": 3600},
                          "list": [
                            %s,
                            %s,
                            %s
                          ]
                        }
                        """.formatted(
                                forecastEntry(fecha.atTime(7, 0), zonaHoraria, 7),
                                forecastEntry(fecha.atTime(14, 0), zonaHoraria, 14),
                                forecastEntry(fecha.plusDays(1).atTime(0, 0), zonaHoraria, 16)
                        ),
                        MediaType.APPLICATION_JSON
                ));

        var result = service.consultarPronostico("Tuxtla Gutierrez", fecha);

        assertTrue(result.isPresent());
        assertEquals(14, result.orElseThrow().respuesta().temperatura());
        assertEquals("pronóstico 14:00", result.orElseThrow().respuesta().descripcion());
        assertEquals(800, result.orElseThrow().codigoCondicion());
        server.verify();
    }

    @Test
    void forecastSinEntradaEnLaFechaSolicitadaDevuelveVacio() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenWeatherService service = new OpenWeatherService(
                builder,
                "api-key",
                "https://api.openweathermap.org/geo/1.0",
                "https://api.openweathermap.org/data/2.5"
        );
        LocalDate targetDate = LocalDate.of(2026, 1, 15);
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/geo/1.0/direct?"
                )))
                .andRespond(withSuccess(
                        "[{\"name\":\"Tuxtla Gutiérrez\",\"lat\":16.75,\"lon\":-93.12,\"country\":\"MX\"}]",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo(Matchers.startsWith(
                        "https://api.openweathermap.org/data/2.5/forecast?"
                )))
                .andRespond(withSuccess(
                        """
                        {
                          "city": {"timezone": -21600},
                          "list": [
                            %s
                          ]
                        }
                        """.formatted(forecastEntry(targetDate.plusDays(1).atTime(12, 0), -21600, 25)),
                        MediaType.APPLICATION_JSON
                ));

        assertTrue(service.consultarPronostico("Tuxtla Gutierrez", targetDate).isEmpty());
        server.verify();
    }

    @Test
    void apiKeyAusenteSeReportaSinLlamarAlProveedor() {
        OpenWeatherService service = new OpenWeatherService(
                RestClient.builder(),
                "",
                "https://api.openweathermap.org/geo/1.0",
                "https://api.openweathermap.org/data/2.5"
        );

        OpenWeatherException exception = assertThrows(
                OpenWeatherException.class,
                () -> service.consultarClima("Tuxtla Gutierrez")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
    }

    private static String forecastEntry(LocalDateTime localDateTime, int timezone, int temperature) {
        long timestamp = localDateTime.toEpochSecond(ZoneOffset.UTC) - timezone;
        return """
                {
                  "dt": %d,
                  "main": {"temp": %d, "feels_like": %d, "humidity": 50},
                  "weather": [{"id": 800, "main": "Clear", "description": "pronóstico %d:00"}],
                  "wind": {"speed": 3}
                }
                """.formatted(timestamp, temperature, temperature, temperature);
    }
}
