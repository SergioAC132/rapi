package com.api.rapi.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail manejarDatosInvalidos(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail manejarConflicto(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ProblemDetail manejarDatoFaltante(InvalidDataAccessApiUsageException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Faltan identificadores obligatorios en la solicitud");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail manejarIntegridad(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "El registro está relacionado con otros datos "
                + "y no se puede modificar o eliminar");
    }
}
