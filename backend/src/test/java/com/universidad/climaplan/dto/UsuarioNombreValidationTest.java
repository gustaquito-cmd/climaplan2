package com.universidad.climaplan.dto;

import com.universidad.climaplan.dto.auth.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioNombreValidationTest {

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
    void aceptaNombresUnicodeConEspaciosApostrofesYGuionesEnTodosLosDtoDeEntrada() {
        List<String> nombresValidos = List.of(
                "Gustavo Arturo Ramos",
                "José Luis",
                "María-José",
                "O'Connor",
                "Al",
                "A".repeat(80)
        );

        for (String nombre : nombresValidos) {
            for (Object request : solicitudesDeUsuario(nombre)) {
                assertTrue(validator.validate(request).isEmpty(), "Nombre rechazado: " + nombre);
            }
        }
    }

    @Test
    void rechazaNumerosSimbolosNombreVacioYLongitudesFueraDeRango() {
        List<String> nombresInvalidos = List.of(
                "Gustavo123",
                "@Gustavo",
                "Gustavo!!!",
                "<script>",
                "",
                "A",
                "A".repeat(81)
        );

        for (String nombre : nombresInvalidos) {
            for (Object request : solicitudesDeUsuario(nombre)) {
                assertFalse(validator.validate(request).isEmpty(), "Nombre aceptado: " + nombre);
            }
        }
    }

    private List<Object> solicitudesDeUsuario(String nombre) {
        return List.of(
                new RegisterRequest(nombre, "gustavo@example.com", "ClaveSegura123"),
                new UsuarioRequest(nombre, "gustavo@example.com", "ClaveSegura123"),
                new UsuarioUpdateRequest(nombre, "gustavo@example.com")
        );
    }
}
