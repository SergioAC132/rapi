package com.api.rapi.dto;

import java.math.BigDecimal;

public record ProductoRequest(
        Long restauranteId,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Boolean disponible) {
}
