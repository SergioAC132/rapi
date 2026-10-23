package com.api.rapi.dto;

public record DetallePedidoRequest(
        Long pedidoId,
        Long productoId,
        Integer cantidad) {
}
