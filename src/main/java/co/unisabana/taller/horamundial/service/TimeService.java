package co.unisabana.taller.horamundial.service;

import co.unisabana.taller.horamundial.dto.TimeRequest;
import co.unisabana.taller.horamundial.dto.TimeResponse;
import co.unisabana.taller.horamundial.exception.TimeApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.ClientResponse;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Service
public class TimeService {
    private static final Logger log = LoggerFactory.getLogger(TimeService.class);
    private final WebClient webClient;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", new Locale("es", "ES"));

    public TimeService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Retryable(value = { TimeoutException.class, ConnectException.class, SocketTimeoutException.class },
              maxAttempts = 3,
              backoff = @org.springframework.retry.annotation.Backoff(delay = 1000, multiplier = 2))
    public Mono<TimeResponse> getCurrentTime(TimeRequest request) {
        // Validar que los parámetros no sean nulos
        if (request == null || request.getCountry() == null || request.getCountry().trim().isEmpty()) {
            return Mono.error(new TimeApiException("El país es requerido"));
        }
        
        String timezone = formatTimezone(request.getCountry(), 
            request.getCity() != null ? request.getCity() : "");
            
        int attempt = RetrySynchronizationManager.getContext() != null ? 
                    RetrySynchronizationManager.getContext().getRetryCount() + 1 : 1;
        
        log.info("Solicitando hora para la zona horaria: {} (Intento {})", timezone, attempt);
        
        // Si ya tenemos una zona horaria completa (país/ciudad), intentamos obtener la hora directamente
        if (timezone.contains("/") && !timezone.endsWith("/")) {
            log.debug("Buscando hora para zona horaria específica: {}", timezone);
            return fetchTimeForZone(timezone)
                    .onErrorResume(e -> {
                        log.warn("No se pudo obtener la hora para {}: {}", timezone, e.getMessage());
                        return Mono.error(new TimeApiException("No se pudo obtener la hora para la zona horaria especificada"));
                    });
        }
        
        // Si solo tenemos el país, buscamos zonas horarias que coincidan
        log.debug("Buscando zonas horarias para: {}", timezone);
        return fetchTimeZones()
                .flatMap(zones -> {
                    // Filtrar zonas que coincidan con el país
                    List<String> matchingZones = new ArrayList<>();
                    for (String zone : zones) {
                        if (zone.startsWith(timezone)) {
                            matchingZones.add(zone);
                        }
                    }
                    
                    if (matchingZones.isEmpty()) {
                        log.warn("No se encontraron zonas horarias para: {}", timezone);
                        return Mono.error(new TimeApiException("No se encontraron zonas horarias para el país especificado"));
                    }
                    
                    // Si solo hay una zona, usarla directamente
                    if (matchingZones.size() == 1) {
                        String zone = matchingZones.get(0);
                        log.debug("Usando única zona horaria encontrada: {}", zone);
                        return fetchTimeForZone(zone);
                    }
                    
                    // Si hay múltiples zonas, devolver un error con las opciones
                    log.warn("Múltiples zonas horarias encontradas para {}: {}", timezone, matchingZones);
                    String message = String.format("Múltiples zonas horarias encontradas. Por favor, especifique una de: %s", 
                            String.join(", ", matchingZones));
                    return Mono.error(new TimeApiException(message));
                })
                .onErrorResume(e -> handleError(e, request));
    }
    
    private Mono<List<String>> fetchTimeZones() {
        log.debug("Obteniendo lista de zonas horarias disponibles");
        
        return webClient.get()
                .uri("/")
                .retrieve()
                .onStatus(status -> status.is5xxServerError() || status.is4xxClientError(),
                         response -> handleErrorResponse(response, "al obtener la lista de zonas horarias"))
                .bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(body);
                        
                        if (rootNode.isArray()) {
                            List<String> zones = new ArrayList<>();
                            rootNode.elements().forEachRemaining(node -> zones.add(node.asText()));
                            return Mono.just(zones);
                        } else {
                            return Mono.error(new TimeApiException("Formato de respuesta inesperado al obtener zonas horarias"));
                        }
                    } catch (Exception e) {
                        log.error("Error al procesar la lista de zonas horarias: {}", body, e);
                        return Mono.error(new TimeApiException("Error al procesar la lista de zonas horarias"));
                    }
                });
    }
    
    private Mono<TimeResponse> fetchTimeForZone(String timezone) {
        log.debug("Obteniendo hora para la zona: {}", timezone);
        
        return webClient.get()
                .uri("/{timezone}", timezone)
                .retrieve()
                .onStatus(status -> status.is5xxServerError() || status.is4xxClientError(),
                         response -> handleErrorResponse(response, "al obtener la hora para " + timezone))
                .bodyToMono(String.class)
                .doOnNext(body -> log.debug("Respuesta en bruto para {}: {}", timezone, body))
                .flatMap(body -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(body);
                        return mapToTimeResponse(jsonNode);
                    } catch (Exception e) {
                        log.error("Error al parsear la respuesta JSON para {}: {}", timezone, body, e);
                        return Mono.error(new TimeApiException("La respuesta del servidor no es un JSON válido"));
                    }
                })
                .timeout(Duration.ofSeconds(10), Mono.error(new TimeoutException("Tiempo de espera agotado")))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(retrySignal -> log.warn("Reintentando después de error: {}", 
                                retrySignal.failure() != null ? retrySignal.failure().getMessage() : "sin mensaje"))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Se agotaron los reintentos para {}", timezone);
                            return new TimeApiException("No se pudo obtener la hora después de varios intentos. Por favor, intente nuevamente más tarde.");
                        }));
    }
    
    private Mono<? extends Throwable> handleErrorResponse(ClientResponse response, String context) {
        log.error("Error en la respuesta HTTP ({}): {}", context, response.statusCode());
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    log.error("Cuerpo de la respuesta de error ({}): {}", context, body);
                    return Mono.error(new RuntimeException("Error " + context + ": " + response.statusCode() + " - " + body));
                });
    }
    
    private boolean isRetryableException(Throwable throwable) {
        // Verificar si el mensaje de error contiene texto específico de cierre prematuro
        boolean isPrematureClose = throwable.getMessage() != null && 
                                 (throwable.getMessage().contains("prematurely closed") ||
                                  throwable.getMessage().contains("Connection reset"));
                                  
        return throwable instanceof TimeoutException ||
               throwable instanceof ConnectException ||
               throwable instanceof SocketTimeoutException ||
               isPrematureClose ||
               (throwable.getCause() != null && isRetryableException(throwable.getCause()));
    }
    
    private Mono<TimeResponse> handleError(Throwable e, TimeRequest request) {
        String errorMsg = String.format("Error al obtener la hora para %s/%s: %s", 
                request.getCountry(), request.getCity(), e.getMessage());
        log.error(errorMsg, e);
        
        if (e instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) e;
            return Mono.error(new TimeApiException(
                String.format("Error al consultar la API de hora. Código: %d, Mensaje: %s", 
                    ex.getStatusCode().value(), 
                    ex.getStatusText())
            ));
        } else if (e instanceof TimeoutException || e instanceof SocketTimeoutException) {
            return Mono.error(new TimeApiException("Tiempo de espera agotado al intentar conectar con el servidor de hora."));
        } else if (e instanceof ConnectException) {
            return Mono.error(new TimeApiException("No se pudo conectar al servidor de hora. Por favor, verifica tu conexión a internet."));
        } else if (e.getMessage() != null && e.getMessage().contains("prematurely closed")) {
            return Mono.error(new TimeApiException("La conexión con el servidor de hora se cerró inesperadamente. Por favor, intente nuevamente."));
        } else if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
            return Mono.error(new TimeApiException("La conexión con el servidor de hora fue reiniciada. Por favor, intente nuevamente."));
        } else {
            return Mono.error(new TimeApiException("Error inesperado al obtener la hora: " + e.getMessage()));
        }
    }

    private Mono<TimeResponse> mapToTimeResponse(JsonNode jsonNode) {
        try {
            log.debug("Respuesta JSON recibida: {}", jsonNode);
            
            // Verificar si los campos requeridos existen
            if (jsonNode == null) {
                log.error("La respuesta de la API es nula");
                return Mono.error(new TimeApiException("La respuesta del servidor está vacía o es inválida"));
            }
            
            JsonNode datetimeNode = jsonNode.get("datetime");
            JsonNode timezoneNode = jsonNode.get("timezone");
            
            if (datetimeNode == null || timezoneNode == null) {
                log.error("Campos faltantes en la respuesta. Se esperaban 'datetime' y 'timezone'. Respuesta: {}", jsonNode);
                return Mono.error(new TimeApiException("La respuesta del servidor no contiene los datos esperados"));
            }
            
            String datetime = datetimeNode.asText();
            String timezone = timezoneNode.asText();
            
            if (datetime == null || timezone == null) {
                log.error("Los campos 'datetime' o 'timezone' están vacíos. Respuesta: {}", jsonNode);
                return Mono.error(new TimeApiException("Los datos de fecha y hora recibidos son inválidos"));
            }
            
            // Extraer ciudad y país del timezone (formato: Area/Location)
            String[] parts = timezone.split("/");
            String country = parts[0];
            String city = parts.length > 1 ? parts[1].replace("_", " ") : "";
            
            // Formatear fecha y hora
            ZonedDateTime zonedDateTime;
            try {
                zonedDateTime = ZonedDateTime.parse(datetime, DATE_TIME_FORMATTER);
            } catch (Exception e) {
                log.error("Error al analizar la fecha/hora '{}' con el formato {}", datetime, DATE_TIME_FORMATTER, e);
                return Mono.error(new TimeApiException("Formato de fecha/hora inválido recibido del servidor"));
            }
            
            String formattedDatetime = zonedDateTime.format(DISPLAY_FORMATTER);
            
            // Determinar si es de día (entre 6 AM y 6 PM)
            int hour = zonedDateTime.getHour();
            boolean isDaytime = hour >= 6 && hour < 18;
            
            TimeResponse response = TimeResponse.builder()
                    .datetime(datetime)
                    .timezone(timezone)
                    .city(city)
                    .country(country)
                    .formattedDatetime(formattedDatetime)
                    .isDaytime(isDaytime)
                    .build();
                    
            log.debug("Respuesta mapeada: {}", response);
            return Mono.just(response);
                    
        } catch (Exception e) {
            log.error("Error inesperado al procesar la respuesta de la API. Respuesta: {}", jsonNode, e);
            return Mono.error(new TimeApiException("Error al procesar la respuesta del servidor: " + e.getMessage()));
        }
    }

    private String formatTimezone(String country, String city) {
        // Formatear el país y la ciudad para la API (ej: America/New_York)
        return String.format("%s/%s", 
                country.trim().replace(" ", "_"), 
                city.trim().replace(" ", "_"));
    }
}
