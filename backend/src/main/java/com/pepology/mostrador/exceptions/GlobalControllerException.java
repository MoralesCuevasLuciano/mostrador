package com.pepology.mostrador.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

	/** 413 cuando el archivo de foto supera los 5 MB. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ProblemDetail fileTooLarge(MaxUploadSizeExceededException ex) {
		return ProblemDetail.forStatusAndDetail(
				HttpStatus.PAYLOAD_TOO_LARGE,
				"La imagen no puede pesar más de 5 MB");
	}
}
