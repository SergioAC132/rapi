package com.api.rapi.integration.facturacion;

public record ResultadoFacturacion(
        boolean exito,
        String folioFiscal,
        String referencia,
        String mensaje) {
}
