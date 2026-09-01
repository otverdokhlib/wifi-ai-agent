package dev.tverdokhlib.wifi_ai_agent.routeros;

import org.springframework.http.HttpStatus;

public class RouterOsException extends RuntimeException {

	private final HttpStatus status;

	public RouterOsException(HttpStatus status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public static RouterOsException authenticationFailed(String message) {
		return new RouterOsException(HttpStatus.UNAUTHORIZED, message, null);
	}

	public static RouterOsException unavailable(String message, Throwable cause) {
		return new RouterOsException(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
	}

	public static RouterOsException requestFailed(String message, Throwable cause) {
		return new RouterOsException(HttpStatus.BAD_GATEWAY, message, cause);
	}
}
