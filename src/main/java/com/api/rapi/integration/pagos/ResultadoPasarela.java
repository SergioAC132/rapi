package com.api.rapi.integration.pagos;

public record ResultadoPasarela(
        boolean aprobado,
        String referencia,
        String mensaje) {
}
