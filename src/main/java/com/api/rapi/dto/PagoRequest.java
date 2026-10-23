package com.api.rapi.dto;

public record PagoRequest(
        Long pedidoId,
        String tokenTarjeta) {
}
