package com.games.shipmaster.common.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.games.shipmaster.common.domain.ConflictException;
import com.games.shipmaster.common.domain.DomainException;
import com.games.shipmaster.common.domain.InvalidInputException;
import com.games.shipmaster.common.domain.NotFoundException;
import com.games.shipmaster.common.domain.UnauthorizedException;

/**
 * Turns every error into RFC 9457 problem+json. Extends Spring's handler so framework errors
 * (unreadable body, wrong method, ...) keep their standard problem responses.
 */
@RestControllerAdvice
public class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        HttpStatus status = statusOf(ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(ProblemTypes.of(ex.problemSlug()));
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setType(ProblemTypes.of("invalid-input"));
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static HttpStatus statusOf(DomainException ex) {
        return switch (ex) {
            case NotFoundException e -> HttpStatus.NOT_FOUND;
            case ConflictException e -> HttpStatus.CONFLICT;
            case InvalidInputException e -> HttpStatus.BAD_REQUEST;
            case UnauthorizedException e -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
