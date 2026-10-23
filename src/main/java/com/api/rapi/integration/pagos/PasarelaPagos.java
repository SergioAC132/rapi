package com.api.rapi.integration.pagos;

import java.math.BigDecimal;

public interface PasarelaPagos {

    ResultadoPasarela cobrar(
            Long pedidoId,
            BigDecimal monto,
            String tokenTarjeta);

    ResultadoPasarela reembolsar(
            String referenciaCobro,
            BigDecimal monto);

}
