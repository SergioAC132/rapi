package com.api.rapi.events;

import java.time.LocalDateTime;

public record EntregaActualizadaEvent(
        Long entregaId,
        Long pedidoId,
        String estado,
        Long repartidorId,
        Double latitud,
        Double longitud,
        LocalDateTime fecha) {
}
