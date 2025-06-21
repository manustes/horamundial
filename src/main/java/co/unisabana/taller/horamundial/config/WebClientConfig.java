package co.unisabana.taller.horamundial.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);
    
    @Bean
    public WebClient webClient() {
        // Configuración del proveedor de conexiones con un pool de conexiones
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // Configuración de timeouts y opciones de conexión
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10 segundos
                .responseTimeout(Duration.ofSeconds(10)) // 10 segundos
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                       .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)))
                // Habilitar keep-alive
                .keepAlive(true)
                // Configuración adicional para manejo de errores
                .doOnRequest((req, conn) -> {
                    conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(10, TimeUnit.SECONDS));
                    conn.addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(10, TimeUnit.SECONDS));
                });
        
        // Configuración de la estrategia de intercambio con buffer más grande
        final int size = 32 * 1024 * 1024; // 32MB
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> {
                    codecs.defaultCodecs().maxInMemorySize(size);
                    codecs.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();
        
        return WebClient.builder()
                .baseUrl("http://worldtimeapi.org/api/timezone")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "WorldTimeAPI Client")
                .filter((request, next) -> {
                    logRequest(request);
                    return next.exchange(request)
                            .doOnNext(response -> logResponse(response));
                })
                .build();
    }
    
    private void logRequest(org.springframework.web.reactive.function.client.ClientRequest request) {
        log.debug("Request: {} {}", request.method(), request.url());
        if (log.isDebugEnabled()) {
            request.headers().forEach((name, values) -> 
                values.forEach(value -> log.debug("{}: {}", name, value))
            );
        }
    }
    
    private void logResponse(org.springframework.web.reactive.function.client.ClientResponse response) {
        log.debug("Response status: {}", response.statusCode());
        if (log.isDebugEnabled()) {
            response.headers().asHttpHeaders().forEach((name, values) -> 
                values.forEach(value -> log.debug("{}: {}", name, value))
            );
        }
    }
}
