package com.api.rapi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.api.rapi.events.PedidoCanceladoEvent;

@Component
public class ReembolsoListener {

    private static final Logger log =
            LoggerFactory.getLogger(ReembolsoListener.class);

    private final PagoService pagoService;

    public ReembolsoListener(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Async
    @TransactionalEventListener
    public void alCancelarPedido(PedidoCanceladoEvent evento) {

        boolean pagado = pagoService.buscarPagoPorPedido(evento.pedidoId())
                .map(pago -> "Confirmado".equals(pago.getEstado()))
                .orElse(false);

        if (!pagado) {
            return;
        }

        try {
            pagoService.reembolsarPago(evento.pedidoId());
        } catch (RuntimeException e) {
            log.error("No se pudo reembolsar el pedido {}: {}",
                    evento.pedidoId(), e.getMessage());
        }
    }
}
