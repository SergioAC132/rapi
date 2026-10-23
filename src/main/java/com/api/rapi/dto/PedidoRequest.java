package com.api.rapi.dto;

public record PedidoRequest(
        Long clienteId,
        Long restauranteId,
        String direccionEntrega) {
}
