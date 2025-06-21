package co.unisabana.taller.horamundial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Tag(name = "Health Check", description = "Endpoints para verificación del estado del servicio")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Verificar estado del servicio", 
               description = "Endpoint de verificación de latido que devuelve 'pong' si el servicio está en funcionamiento")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
