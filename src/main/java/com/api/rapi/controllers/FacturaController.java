package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.FacturaRequest;
import com.api.rapi.models.FacturaModel;
import com.api.rapi.services.FacturaService;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<FacturaModel> obtenerFacturas() {
        return facturaService.obtenerFacturas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaModel> obtenerFacturaPorId(
            @PathVariable Long id) {

        return facturaService.buscarFacturaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<FacturaModel> buscarPorPedido(
            @PathVariable Long pedidoId) {

        return facturaService.buscarFacturaPorPedido(pedidoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/folio")
    public ResponseEntity<FacturaModel> buscarPorFolio(
            @RequestParam String folioFiscal) {

        return facturaService.buscarFacturaPorFolioFiscal(folioFiscal)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estado")
    public List<FacturaModel> buscarPorEstado(
            @RequestParam String estado) {

        return facturaService.buscarFacturasPorEstado(estado);
    }

    @GetMapping("/rfc")
    public List<FacturaModel> buscarPorRFC(
            @RequestParam String rfcReceptor) {

        return facturaService.buscarFacturasPorRFC(rfcReceptor);
    }

    @PostMapping
    public ResponseEntity<FacturaModel> solicitarFactura(
            @RequestBody FacturaRequest solicitud) {

        FacturaModel nuevaFactura = facturaService.solicitarFactura(
                solicitud.pedidoId(),
                solicitud.rfcReceptor(),
                solicitud.razonSocial(),
                solicitud.correoFacturacion());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(nuevaFactura);
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<FacturaModel> reintentarFactura(
            @PathVariable Long id) {

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(facturaService.reintentarFactura(id));
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<String> consultarEstado(
            @PathVariable Long id) {

        return facturaService.buscarFacturaPorId(id)
                .map(factura -> ResponseEntity.ok(factura.getEstado()))
                .orElse(ResponseEntity.notFound().build());
    }
}
