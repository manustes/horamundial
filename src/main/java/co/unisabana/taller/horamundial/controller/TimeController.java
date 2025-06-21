package co.unisabana.taller.horamundial.controller;

import co.unisabana.taller.horamundial.dto.TimeRequest;
import co.unisabana.taller.horamundial.dto.TimeResponse;
import co.unisabana.taller.horamundial.service.TimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/time")
@Tag(name = "Time API", description = "API para obtener la hora actual de diferentes ubicaciones del mundo")
public class TimeController {

    private final TimeService timeService;
    
    public TimeController(TimeService timeService) {
        this.timeService = timeService;
    }

    @Operation(
        summary = "Obtener la hora actual",
        description = "Obtiene la hora actual para una ciudad y país específicos",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Hora obtenida exitosamente",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TimeResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Parámetros de entrada inválidos o ubicación no encontrada"
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "Error interno del servidor"
            )
        }
    )
    @PostMapping("/current")
    public Mono<ResponseEntity<TimeResponse>> getCurrentTime(@Valid @RequestBody TimeRequest request) {
        return timeService.getCurrentTime(request)
                .map(ResponseEntity::ok);
    }

    @Operation(
        summary = "Obtener la hora actual (versión GET)",
        description = "Versión GET para obtener la hora actual de una ubicación",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Hora obtenida exitosamente",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TimeResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Parámetros de entrada inválidos o ubicación no encontrada"
            )
        }
    )
    @GetMapping("/current")
    public Mono<ResponseEntity<TimeResponse>> getCurrentTimeGet(
            @RequestParam String country,
            @RequestParam String city) {
        
        TimeRequest request = new TimeRequest(country, city);
        return timeService.getCurrentTime(request)
                .map(ResponseEntity::ok);
    }
}
