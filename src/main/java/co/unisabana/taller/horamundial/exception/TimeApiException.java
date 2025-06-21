package co.unisabana.taller.horamundial.exception;

public class TimeApiException extends RuntimeException {
    public TimeApiException(String message) {
        super(message);
    }

    public TimeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
