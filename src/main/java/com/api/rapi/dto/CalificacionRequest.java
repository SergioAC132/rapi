package com.api.rapi.dto;

public record CalificacionRequest(
        Long pedidoId,
        Integer puntuacionRestaurante,
        Integer puntuacionRepartidor,
        String comentario) {
}
