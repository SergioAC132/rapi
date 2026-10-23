package com.api.rapi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.api.rapi.events.PedidoAceptadoEvent;
import com.api.rapi.models.EntregaModel;

@Component
public class AsignacionEntregaListener {

    private static final Logger log =
            LoggerFactory.getLogger(AsignacionEntregaListener.class);

    private final EntregaService entregaService;

    public AsignacionEntregaListener(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @Async
    @TransactionalEventListener
    public void alAceptarPedido(PedidoAceptadoEvent evento) {

        EntregaModel entrega = entregaService.crearEntrega(evento.pedidoId());

        try {
            entregaService.asignarRepartidor(entrega.getId());
        } catch (IllegalStateException e) {
            log.warn("Entrega {} pendiente de asignación: {}",
                    entrega.getId(), e.getMessage());
        }
    }
}
