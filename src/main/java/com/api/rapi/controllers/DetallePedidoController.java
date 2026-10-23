package com.api.rapi.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.CantidadRequest;
import com.api.rapi.dto.DetallePedidoRequest;
import com.api.rapi.models.DetallePedidoModel;
import com.api.rapi.services.DetallePedidoService;

@RestController
@RequestMapping("/api/detalles-pedido")
public class DetallePedidoController {

    private final DetallePedidoService detallePedidoService;

    public DetallePedidoController(
            DetallePedidoService detallePedidoService) {

        this.detallePedidoService = detallePedidoService;
    }

    @GetMapping
    public List<DetallePedidoModel> obtenerDetalles() {
        return detallePedidoService.obtenerDetalles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetallePedidoModel> obtenerDetallePorId(
            @PathVariable Long id) {

        return detallePedidoService.buscarDetallePorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<DetallePedidoModel> obtenerDetallesPorPedido(
            @PathVariable Long pedidoId) {

        return detallePedidoService.obtenerDetallesPorPedido(pedidoId);
    }

    @GetMapping("/producto/{productoId}")
    public List<DetallePedidoModel> obtenerDetallesPorProducto(
            @PathVariable Long productoId) {

        return detallePedidoService.obtenerDetallesPorProducto(productoId);
    }

    @PostMapping
    public ResponseEntity<DetallePedidoModel> agregarProducto(
            @RequestBody DetallePedidoRequest solicitud) {

        DetallePedidoModel nuevoDetalle =
                detallePedidoService.agregarProductoAlPedido(
                        solicitud.pedidoId(),
                        solicitud.productoId(),
                        solicitud.cantidad());

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDetalle);
    }

    @PatchMapping("/{id}/cantidad")
    public DetallePedidoModel actualizarCantidad(
            @PathVariable Long id,
            @RequestBody CantidadRequest solicitud) {

        return detallePedidoService.actualizarCantidad(
                id, solicitud.cantidad());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        detallePedidoService.eliminarProductoDelPedido(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pedido/{pedidoId}/total")
    public BigDecimal calcularTotalPedido(@PathVariable Long pedidoId) {
        return detallePedidoService.calcularTotalPedido(pedidoId);
    }
}
