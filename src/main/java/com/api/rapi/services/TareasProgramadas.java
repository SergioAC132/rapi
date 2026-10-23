package com.api.rapi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TareasProgramadas {

    private static final Logger log =
            LoggerFactory.getLogger(TareasProgramadas.class);

    private final PedidoService pedidoService;
    private final EntregaService entregaService;

    public TareasProgramadas(
            PedidoService pedidoService,
            EntregaService entregaService) {

        this.pedidoService = pedidoService;
        this.entregaService = entregaService;
    }

    @Scheduled(fixedDelayString = "${rapi.pedidos.revision-vencidos-ms:10000}")
    public void cancelarPedidosVencidos() {

        int cancelados = pedidoService.cancelarPedidosVencidos();

        if (cancelados > 0) {
            log.info("Pedidos cancelados por tiempo de aceptación: {}",
                    cancelados);
        }
    }

    @Scheduled(fixedDelayString = "${rapi.entregas.reintento-asignacion-ms:30000}")
    public void reintentarAsignaciones() {

        int asignadas = entregaService.reintentarAsignacionesPendientes();

        if (asignadas > 0) {
            log.info("Entregas asignadas en reintento: {}", asignadas);
        }
    }
}
