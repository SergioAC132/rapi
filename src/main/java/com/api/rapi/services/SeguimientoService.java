package com.api.rapi.services;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.api.rapi.events.EntregaActualizadaEvent;
import com.api.rapi.models.EntregaModel;

@Service
public class SeguimientoService {

    private static final long TIEMPO_CONEXION_MS =
            Duration.ofMinutes(30).toMillis();

    private final Map<Long, List<SseEmitter>> suscriptores =
            new ConcurrentHashMap<>();

    private final EntregaService entregaService;

    public SeguimientoService(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    public SseEmitter suscribir(Long pedidoId) {

        SseEmitter emisor = new SseEmitter(TIEMPO_CONEXION_MS);

        List<SseEmitter> lista = suscriptores.computeIfAbsent(
                pedidoId, id -> new CopyOnWriteArrayList<>());

        lista.add(emisor);

        emisor.onCompletion(() -> lista.remove(emisor));
        emisor.onTimeout(() -> lista.remove(emisor));
        emisor.onError(e -> lista.remove(emisor));

        entregaService.buscarEntregaPorPedido(pedidoId)
                .map(this::aEvento)
                .ifPresent(evento -> enviar(emisor, evento));

        return emisor;
    }

    @TransactionalEventListener
    public void alActualizarEntrega(EntregaActualizadaEvent evento) {

        List<SseEmitter> lista = suscriptores.get(evento.pedidoId());

        if (lista == null) {
            return;
        }

        for (SseEmitter emisor : lista) {
            if (enviar(emisor, evento) &&
                    "Entregada".equals(evento.estado())) {
                emisor.complete();
            }
        }

        if ("Entregada".equals(evento.estado())) {
            suscriptores.remove(evento.pedidoId());
        }
    }

    private boolean enviar(SseEmitter emisor, EntregaActualizadaEvent evento) {

        try {
            emisor.send(SseEmitter.event()
                    .name("entrega")
                    .data(evento, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            emisor.completeWithError(e);
            return false;
        }
    }

    private EntregaActualizadaEvent aEvento(EntregaModel entrega) {

        return new EntregaActualizadaEvent(
                entrega.getId(),
                entrega.getPedido().getId(),
                entrega.getEstado(),
                entrega.getRepartidor() != null
                        ? entrega.getRepartidor().getId() : null,
                entrega.getLatitudActual(),
                entrega.getLongitudActual(),
                LocalDateTime.now());
    }
}
