package com.api.rapi.integration.facturacion;

import java.math.RoundingMode;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.api.rapi.models.FacturaModel;

@Component
public class SistemaFacturacionLegadoAdapter implements SistemaFacturacion {

    private static final String SEPARADOR = Pattern.quote("|");

    @Override
    public ResultadoFacturacion emitirFactura(FacturaModel factura) {

        String solicitud = String.join("|",
                "FAC",
                factura.getPedido().getId().toString(),
                factura.getRfcReceptor(),
                factura.getRazonSocial(),
                factura.getMontoTotal()
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString(),
                factura.getCorreoFacturacion());

        String respuesta = enviarAlSistemaLegado(solicitud);

        return traducirRespuesta(respuesta);
    }

    private String enviarAlSistemaLegado(String solicitud) {

        String[] campos = solicitud.split(SEPARADOR);
        String rfc = campos[2];

        if (rfc.length() < 12 || rfc.length() > 13) {
            return "ERR|E01|RFC INVALIDO";
        }

        return "OK|" + UUID.randomUUID().toString().toUpperCase()
                + "|LEG-" + System.currentTimeMillis();
    }

    private ResultadoFacturacion traducirRespuesta(String respuesta) {

        String[] campos = respuesta.split(SEPARADOR);

        if ("OK".equals(campos[0])) {
            return new ResultadoFacturacion(
                    true, campos[1], campos[2], "Factura emitida");
        }

        return new ResultadoFacturacion(
                false, null, null,
                "Sistema de facturación: " + campos[2]);
    }
}
