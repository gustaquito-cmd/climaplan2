package com.universidad.climaplan.controller;

import com.universidad.climaplan.dto.DestinoFavoritoRequest;
import com.universidad.climaplan.dto.DestinoFavoritoResponse;
import com.universidad.climaplan.dto.error.ApiErrorResponse;
import com.universidad.climaplan.service.DestinoFavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "Destinos favoritos", description = "Gestión de destinos favoritos por usuario")
@SecurityRequirement(name = "bearerAuth")
public class DestinoFavoritoController {

    private final DestinoFavoritoService destinoFavoritoService;

    public DestinoFavoritoController(DestinoFavoritoService destinoFavoritoService) {
        this.destinoFavoritoService = destinoFavoritoService;
    }

    @PostMapping
    @Operation(summary = "Crear destino favorito", description = "Crea un destino para el usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Destino favorito creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DestinoFavoritoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DestinoFavoritoResponse> crear(
            @Valid @RequestBody DestinoFavoritoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(destinoFavoritoService.crear(request, authentication.getName()));
    }

    @GetMapping
    @Operation(summary = "Listar destinos favoritos", description = "Lista los destinos visibles para el usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destinos favoritos obtenidos",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DestinoFavoritoResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<DestinoFavoritoResponse>> obtenerTodos(Authentication authentication) {
        return ResponseEntity.ok(destinoFavoritoService.obtenerTodos(
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar destino favorito", description = "Obtiene un destino respetando su propietario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destino favorito encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DestinoFavoritoResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Destino favorito no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DestinoFavoritoResponse> obtenerPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(destinoFavoritoService.obtenerPorId(
                id,
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar destino favorito", description = "Actualiza un destino respetando su propietario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Destino favorito actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DestinoFavoritoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Destino favorito no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<DestinoFavoritoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DestinoFavoritoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(destinoFavoritoService.actualizar(
                id,
                request,
                authentication.getName(),
                esAdministrador(authentication)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar destino favorito", description = "Elimina un destino respetando su propietario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Destino favorito eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Destino favorito no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication authentication) {
        destinoFavoritoService.eliminar(
                id,
                authentication.getName(),
                esAdministrador(authentication)
        );
        return ResponseEntity.noContent().build();
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
