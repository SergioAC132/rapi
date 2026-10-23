package com.api.rapi.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.PedidoRequest;
import com.api.rapi.models.PedidoModel;
import com.api.rapi.services.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<PedidoModel> obtenerPedidos() {
        return pedidoService.obtenerPedidos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoModel> obtenerPedidoPorId(
            @PathVariable Long id) {

        return pedidoService.buscarPedidoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public List<PedidoModel> buscarPorCliente(
            @PathVariable Long clienteId) {

        return pedidoService.buscarPedidosPorCliente(clienteId);
    }

    @GetMapping("/restaurante/{restauranteId}")
    public List<PedidoModel> buscarPorRestaurante(
            @PathVariable Long restauranteId) {

        return pedidoService.buscarPedidosPorRestaurante(restauranteId);
    }

    @GetMapping("/estado")
    public List<PedidoModel> buscarPorEstado(
            @RequestParam String estado) {

        return pedidoService.buscarPedidosPorEstado(estado);
    }

    @GetMapping("/restaurante/{restauranteId}/estado")
    public List<PedidoModel> buscarPorRestauranteYEstado(
            @PathVariable Long restauranteId,
            @RequestParam String estado) {

        return pedidoService.buscarPedidosPorRestauranteYEstado(
                restauranteId, estado);
    }

    @PostMapping
    public ResponseEntity<PedidoModel> crearPedido(
            @RequestBody PedidoRequest solicitud) {

        PedidoModel nuevoPedido = pedidoService.crearPedido(
                solicitud.clienteId(),
                solicitud.restauranteId(),
                solicitud.direccionEntrega());

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPedido);
    }

    @PatchMapping("/{id}/aceptar")
    public PedidoModel aceptarPedido(
            @PathVariable Long id,
            @RequestParam Long restauranteId) {

        return pedidoService.aceptarPedido(id, restauranteId);
    }

    @PatchMapping("/{id}/rechazar")
    public PedidoModel rechazarPedido(
            @PathVariable Long id,
            @RequestParam Long restauranteId) {

        return pedidoService.rechazarPedido(id, restauranteId);
    }

    @PatchMapping("/{id}/estado")
    public PedidoModel cambiarEstadoPedido(
            @PathVariable Long id,
            @RequestParam String nuevoEstado) {

        return pedidoService.cambiarEstadoPedido(id, nuevoEstado);
    }

    @PatchMapping("/{id}/cancelar")
    public PedidoModel cancelarPedido(@PathVariable Long id) {
        return pedidoService.cancelarPedido(id);
    }

    @GetMapping("/vencidos")
    public List<PedidoModel> obtenerPedidosVencidos() {

        return pedidoService.obtenerPedidosConAceptacionVencida(
                LocalDateTime.now());
    }
}
