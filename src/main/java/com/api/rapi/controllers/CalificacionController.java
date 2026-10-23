package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.CalificacionRequest;
import com.api.rapi.models.CalificacionModel;
import com.api.rapi.services.CalificacionService;

@RestController
@RequestMapping("/api/calificaciones")
public class CalificacionController {

    private final CalificacionService calificacionService;

    public CalificacionController(
            CalificacionService calificacionService) {

        this.calificacionService = calificacionService;
    }

    @GetMapping
    public List<CalificacionModel> obtenerCalificaciones() {
        return calificacionService.obtenerCalificaciones();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalificacionModel> obtenerCalificacionPorId(
            @PathVariable Long id) {

        return calificacionService.buscarCalificacionPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<CalificacionModel> buscarPorPedido(
            @PathVariable Long pedidoId) {

        return calificacionService.buscarCalificacionPorPedido(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurante/{restauranteId}")
    public List<CalificacionModel> buscarPorRestaurante(
            @PathVariable Long restauranteId) {

        return calificacionService.buscarCalificacionesPorRestaurante(
                restauranteId);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<CalificacionModel> buscarPorCliente(
            @PathVariable Long clienteId) {

        return calificacionService.buscarCalificacionesPorCliente(
                clienteId);
    }

    @PostMapping
    public ResponseEntity<CalificacionModel> registrarCalificacion(
            @RequestBody CalificacionRequest solicitud) {

        CalificacionModel nuevaCalificacion =
                calificacionService.registrarCalificacion(
                        solicitud.pedidoId(),
                        solicitud.puntuacionRestaurante(),
                        solicitud.puntuacionRepartidor(),
                        solicitud.comentario());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nuevaCalificacion);
    }

    @PutMapping("/{id}")
    public CalificacionModel actualizarCalificacion(
            @PathVariable Long id,
            @RequestBody CalificacionRequest solicitud) {

        return calificacionService.actualizarCalificacion(
                id,
                solicitud.puntuacionRestaurante(),
                solicitud.puntuacionRepartidor(),
                solicitud.comentario());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCalificacion(
            @PathVariable Long id) {

        calificacionService.eliminarCalificacion(id);

        return ResponseEntity.noContent().build();
    }
}
