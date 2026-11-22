package com.universidad.climaplan.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DestinoFavoritoRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @Test
    void aceptaLosLimitesDeLatitudYLongitud() {
        DestinoFavoritoRequest request =
                new DestinoFavoritoRequest("Ciudad", "País", -90.0, 180.0);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rechazaLatitudYLongitudFueraDeRango() {
        DestinoFavoritoRequest request =
                new DestinoFavoritoRequest("Ciudad", "País", 90.01, -180.01);

        Set<String> camposInvalidos = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("latitud", "longitud"), camposInvalidos);
    }
}
