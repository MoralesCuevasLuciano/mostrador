package com.pepology.mostrador.exceptions;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte excepciones de dominio en respuestas HTTP (ProblemDetail).
 */
@RestControllerAdvice
public class GlobalControllerException {

	/** 404 cuando no existe la entidad pedida. */
	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** 409 cuando una regla de negocio impide la operación. */
	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail businessRule(BusinessRuleException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/** 405 cuando el verbo no corresponde a esa URL. */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ProblemDetail methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
		Set<String> allowed = ex.getSupportedHttpMethods() == null
				? Set.of()
				: ex.getSupportedHttpMethods().stream().map(HttpMethod::name).collect(Collectors.toSet());
		String detail = allowed.isEmpty()
				? "Esta URL no acepta " + ex.getMethod()
				: "Esta URL no acepta " + ex.getMethod() + ". Métodos válidos: " + String.join(", ", allowed);
		return ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, detail);
	}

	/** 400 cuando el body no pasa la validación de campos. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		if (detail.isBlank()) {
			detail = "El cuerpo del pedido no es válido";
		}
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

	/** 413 cuando el archivo de foto supera los 5 MB. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ProblemDetail fileTooLarge(MaxUploadSizeExceededException ex) {
		return ProblemDetail.forStatusAndDetail(
				HttpStatus.PAYLOAD_TOO_LARGE,
				"La imagen no puede pesar más de 5 MB");
	}
}
