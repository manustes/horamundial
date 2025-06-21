package co.unisabana.taller.horamundial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class WorldTimeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorldTimeApplication.class, args);
	}

}
