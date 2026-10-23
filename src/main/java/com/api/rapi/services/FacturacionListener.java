package com.api.rapi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.api.rapi.events.FacturaSolicitadaEvent;

@Component
public class FacturacionListener {

    private static final Logger log =
            LoggerFactory.getLogger(FacturacionListener.class);

    private final FacturaService facturaService;

    public FacturacionListener(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Async
    @TransactionalEventListener
    public void alSolicitarFactura(FacturaSolicitadaEvent evento) {

        try {
            facturaService.procesarFactura(evento.facturaId());
        } catch (RuntimeException e) {
            log.error("No se pudo procesar la factura {}: {}",
                    evento.facturaId(), e.getMessage());
        }
    }
}
