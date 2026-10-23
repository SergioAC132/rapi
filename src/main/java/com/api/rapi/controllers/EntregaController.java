package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.api.rapi.dto.UbicacionRequest;
import com.api.rapi.models.EntregaModel;
import com.api.rapi.services.EntregaService;
import com.api.rapi.services.SeguimientoService;

@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private final EntregaService entregaService;
    private final SeguimientoService seguimientoService;

    public EntregaController(
            EntregaService entregaService,
            SeguimientoService seguimientoService) {

        this.entregaService = entregaService;
        this.seguimientoService = seguimientoService;
    }

    @GetMapping
    public List<EntregaModel> obtenerEntregas() {
        return entregaService.obtenerEntregas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntregaModel> obtenerEntregaPorId(
            @PathVariable Long id) {

        return entregaService.buscarEntregaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<EntregaModel> buscarPorPedido(
            @PathVariable Long pedidoId) {

        return entregaService.buscarEntregaPorPedido(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/pedido/{pedidoId}/seguimiento",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter seguirEntrega(@PathVariable Long pedidoId) {
        return seguimientoService.suscribir(pedidoId);
    }

    @GetMapping("/repartidor/{repartidorId}")
    public List<EntregaModel> buscarPorRepartidor(
            @PathVariable Long repartidorId) {

        return entregaService.buscarEntregasPorRepartidor(repartidorId);
    }

    @GetMapping("/estado")
    public List<EntregaModel> buscarPorEstado(
            @RequestParam String estado) {

        return entregaService.buscarEntregasPorEstado(estado);
    }

    @PostMapping("/pedido/{pedidoId}")
    public ResponseEntity<EntregaModel> crearEntrega(
            @PathVariable Long pedidoId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregaService.crearEntrega(pedidoId));
    }

    @PatchMapping("/{id}/asignar")
    public EntregaModel asignarRepartidor(@PathVariable Long id) {
        return entregaService.asignarRepartidor(id);
    }

    @PatchMapping("/{id}/asignar/{repartidorId}")
    public EntregaModel asignarRepartidorManual(
            @PathVariable Long id,
            @PathVariable Long repartidorId) {

        return entregaService.asignarRepartidorManual(id, repartidorId);
    }

    @PatchMapping("/{id}/recogida")
    public EntregaModel confirmarRecogida(@PathVariable Long id) {
        return entregaService.confirmarRecogida(id);
    }

    @PatchMapping("/{id}/iniciar")
    public EntregaModel iniciarEntrega(@PathVariable Long id) {
        return entregaService.iniciarEntrega(id);
    }

    @PatchMapping("/{id}/ubicacion")
    public EntregaModel actualizarUbicacion(
            @PathVariable Long id,
            @RequestBody UbicacionRequest ubicacion) {

        return entregaService.actualizarUbicacion(
                id, ubicacion.latitud(), ubicacion.longitud());
    }

    @PatchMapping("/{id}/confirmar")
    public EntregaModel confirmarEntrega(@PathVariable Long id) {
        return entregaService.confirmarEntrega(id);
    }
}
