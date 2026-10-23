package com.api.rapi.integration.facturacion;

import com.api.rapi.models.FacturaModel;

public interface SistemaFacturacion {

    ResultadoFacturacion emitirFactura(FacturaModel factura);

}
