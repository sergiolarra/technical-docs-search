package com.kata.docsearch.infrastructure.adapter.in.web;

import com.kata.docsearch.domain.exception.DocumentNotFoundException;
import com.kata.docsearch.domain.exception.InvalidDocumentException;
import com.kata.docsearch.domain.exception.InvalidSearchQueryException;
import com.kata.docsearch.domain.exception.UnsupportedDocumentFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** All errors are returned in the same format (ProblemDetail). The base class already covers
 * Spring MVC errors: missing multipart part (400), size exceeded (413), media type (415)...
 */
@Slf4j
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidDocumentException.class)
    ProblemDetail handleInvalidDocument(InvalidDocumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid document", e.getMessage());
    }

    @ExceptionHandler(UnsupportedDocumentFormatException.class)
    ProblemDetail handleUnsupportedFormat(UnsupportedDocumentFormatException e) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported document format", e.getMessage());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    ProblemDetail handleNotFound(DocumentNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Document not found", e.getMessage());
    }

    @ExceptionHandler(AmqpException.class)
    ProblemDetail handleBrokerUnavailable(AmqpException e) {
        log.error("Message broker unavailable", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Processing service unavailable",
                "The document could not be queued. Please try again later.");
    }

    /** Last resort: never expose stack traces or internal details to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred");
    }

    @ExceptionHandler(InvalidSearchQueryException.class)
    ProblemDetail handleInvalidSearchQuery(InvalidSearchQueryException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid search query", e.getMessage());
    }

    /** Adds field errors to the body: {"errors": {"title": "must not be blank"}}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value"));
        }
        ProblemDetail body = ex.getBody();
        body.setTitle("Validation failed");
        body.setDetail("One or more fields are invalid");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}