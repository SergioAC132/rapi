package com.api.rapi.integration.pagos;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PasarelaPagosSimulada implements PasarelaPagos {

    private static final String TOKEN_RECHAZADO = "tok_rechazada";

    @Override
    public ResultadoPasarela cobrar(
            Long pedidoId,
            BigDecimal monto,
            String tokenTarjeta) {

        if (TOKEN_RECHAZADO.equals(tokenTarjeta)) {
            return new ResultadoPasarela(
                    false, null, "Tarjeta rechazada por el banco emisor");
        }

        return new ResultadoPasarela(
                true, "ch_" + UUID.randomUUID(), "Cargo aprobado");
    }

    @Override
    public ResultadoPasarela reembolsar(
            String referenciaCobro,
            BigDecimal monto) {

        return new ResultadoPasarela(
                true, "re_" + UUID.randomUUID(), "Reembolso aplicado");
    }
}
