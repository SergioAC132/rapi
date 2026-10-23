package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.PagoRequest;
import com.api.rapi.models.PagoModel;
import com.api.rapi.services.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    public List<PagoModel> obtenerPagos() {
        return pagoService.obtenerPagos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoModel> obtenerPagoPorId(
            @PathVariable Long id) {

        return pagoService.buscarPagoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<PagoModel> obtenerPagoPorPedido(
            @PathVariable Long pedidoId) {

        return pagoService.buscarPagoPorPedido(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/referencia")
    public ResponseEntity<PagoModel> buscarPorReferencia(
            @RequestParam String referenciaExterna) {

        return pagoService.buscarPagoPorReferencia(referenciaExterna)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado")
    public List<PagoModel> buscarPagosPorEstado(
            @RequestParam String estado) {

        return pagoService.buscarPagosPorEstado(estado);
    }

    @PostMapping
    public ResponseEntity<PagoModel> procesarPago(
            @RequestBody PagoRequest solicitud) {

        PagoModel pago = pagoService.procesarPago(
                solicitud.pedidoId(), solicitud.tokenTarjeta());

        HttpStatus estado = "Fallido".equals(pago.getEstado())
                ? HttpStatus.PAYMENT_REQUIRED
                : HttpStatus.CREATED;

        return ResponseEntity.status(estado).body(pago);
    }

    @PostMapping("/pedido/{pedidoId}/reembolso")
    public PagoModel reembolsarPago(@PathVariable Long pedidoId) {
        return pagoService.reembolsarPago(pedidoId);
    }

    @GetMapping("/pedido/{pedidoId}/confirmado")
    public boolean verificarPagoConfirmado(@PathVariable Long pedidoId) {
        return pagoService.verificarPagoConfirmado(pedidoId);
    }
}
