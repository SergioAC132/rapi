package com.api.rapi.dto;

public record FacturaRequest(
        Long pedidoId,
        String rfcReceptor,
        String razonSocial,
        String correoFacturacion) {
}
