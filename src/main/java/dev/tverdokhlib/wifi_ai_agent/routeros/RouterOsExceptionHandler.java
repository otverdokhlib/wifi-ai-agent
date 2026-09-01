package dev.tverdokhlib.wifi_ai_agent.routeros;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RouterOsExceptionHandler {

	@ExceptionHandler(RouterOsException.class)
	ResponseEntity<Map<String, String>> handleRouterOsException(RouterOsException ex) {
		return ResponseEntity.status(ex.getStatus())
				.body(Map.of("error", ex.getMessage()));
	}
}
