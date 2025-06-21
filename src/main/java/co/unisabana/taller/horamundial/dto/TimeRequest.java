package co.unisabana.taller.horamundial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TimeRequest {
    @NotBlank(message = "El país es requerido")
    @Size(min = 2, max = 100, message = "El país debe tener entre 2 y 100 caracteres")
    private String country;
    
    @NotBlank(message = "La ciudad es requerida")
    @Size(min = 2, max = 100, message = "La ciudad debe tener entre 2 y 100 caracteres")
    private String city;

    public TimeRequest() {
    }

    public TimeRequest(String country, String city) {
        this.country = country;
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
