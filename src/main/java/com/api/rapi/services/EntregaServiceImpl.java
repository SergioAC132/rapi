package com.api.rapi.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.rapi.events.EntregaActualizadaEvent;
import com.api.rapi.exceptions.RecursoNoEncontradoException;
import com.api.rapi.models.EntregaModel;
import com.api.rapi.models.PedidoModel;
import com.api.rapi.models.RepartidorModel;
import com.api.rapi.models.RestauranteModel;

import com.api.rapi.repositories.EntregaRepository;
import com.api.rapi.repositories.RepartidorRepository;

@Service
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository entregaRepository;
    private final RepartidorRepository repartidorRepository;
    private final RepartidorService repartidorService;
    private final PedidoService pedidoService;
    private final ApplicationEventPublisher eventos;

    public EntregaServiceImpl(
            EntregaRepository entregaRepository,
            RepartidorRepository repartidorRepository,
            RepartidorService repartidorService,
            PedidoService pedidoService,
            ApplicationEventPublisher eventos) {

        this.entregaRepository = entregaRepository;
        this.repartidorRepository = repartidorRepository;
        this.repartidorService = repartidorService;
        this.pedidoService = pedidoService;
        this.eventos = eventos;
    }

    @Override
    public List<EntregaModel> obtenerEntregas() {
        return entregaRepository.findAll();
    }

    @Override
    public Optional<EntregaModel> buscarEntregaPorId(Long id) {
        return entregaRepository.findById(id);
    }

    @Override
    public Optional<EntregaModel> buscarEntregaPorPedido(Long pedidoId) {
        return entregaRepository.findByPedidoId(pedidoId);
    }

    @Override
    public List<EntregaModel> buscarEntregasPorRepartidor(Long repartidorId) {
        return entregaRepository.findByRepartidorId(repartidorId);
    }

    @Override
    public List<EntregaModel> buscarEntregasPorEstado(String estado) {
        return entregaRepository.findByEstado(estado);
    }

    @Override
    @Transactional
    public EntregaModel crearEntrega(Long pedidoId) {

        PedidoModel pedido = pedidoService.buscarPedidoPorId(pedidoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El pedido no existe"));

        if (!"Aceptado".equals(pedido.getEstado()) &&
                !"EnPreparacion".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido debe estar aceptado o en preparación");
        }

        if (entregaRepository.existsByPedidoId(pedidoId)) {
            throw new IllegalStateException(
                    "El pedido ya tiene una entrega registrada");
        }

        EntregaModel entrega = new EntregaModel();

        entrega.setPedido(pedido);
        entrega.setEstado("PendienteAsignacion");

        return guardarYNotificar(entrega);
    }

    @Override
    @Transactional
    public EntregaModel asignarRepartidor(Long entregaId) {

        EntregaModel entrega = obtenerEntrega(entregaId);
        validarPendienteAsignacion(entrega);

        RestauranteModel restaurante = entrega.getPedido().getRestaurante();

        if (restaurante.getLatitud() == null ||
                restaurante.getLongitud() == null) {
            throw new IllegalStateException(
                    "El restaurante no tiene ubicación registrada");
        }

        RepartidorModel repartidor = repartidorService
                .buscarRepartidorMasCercano(
                        restaurante.getLatitud(),
                        restaurante.getLongitud())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No hay repartidores disponibles con ubicación"));

        return asignar(entrega, repartidor);
    }

    @Override
    @Transactional
    public EntregaModel asignarRepartidorManual(
            Long entregaId, Long repartidorId) {

        EntregaModel entrega = obtenerEntrega(entregaId);
        validarPendienteAsignacion(entrega);

        RepartidorModel repartidor = repartidorRepository.findById(repartidorId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El repartidor no existe"));

        return asignar(entrega, repartidor);
    }

    @Override
    @Transactional
    public EntregaModel confirmarRecogida(Long entregaId) {

        EntregaModel entrega = obtenerEntrega(entregaId);

        if (!"Asignada".equals(entrega.getEstado())) {
            throw new IllegalStateException(
                    "La entrega debe estar asignada");
        }

        entrega.setEstado("Recogida");
        entrega.setFechaRecogida(LocalDateTime.now());

        return guardarYNotificar(entrega);
    }

    @Override
    @Transactional
    public EntregaModel iniciarEntrega(Long entregaId) {

        EntregaModel entrega = obtenerEntrega(entregaId);

        if (!"Recogida".equals(entrega.getEstado())) {
            throw new IllegalStateException(
                    "Primero se debe confirmar la recogida");
        }

        pedidoService.marcarPedidoEnCamino(entrega.getPedido().getId());

        entrega.setEstado("EnCamino");

        return guardarYNotificar(entrega);
    }

    @Override
    @Transactional
    public EntregaModel actualizarUbicacion(
            Long entregaId, Double latitud, Double longitud) {

        validarCoordenadas(latitud, longitud);

        EntregaModel entrega = obtenerEntrega(entregaId);

        if (!"Asignada".equals(entrega.getEstado()) &&
                !"Recogida".equals(entrega.getEstado()) &&
                !"EnCamino".equals(entrega.getEstado())) {
            throw new IllegalStateException(
                    "Solo se puede actualizar la ubicación de una entrega en curso");
        }

        entrega.setLatitudActual(latitud);
        entrega.setLongitudActual(longitud);

        RepartidorModel repartidor = entrega.getRepartidor();

        repartidor.setLatitud(latitud);
        repartidor.setLongitud(longitud);
        repartidorRepository.save(repartidor);

        return guardarYNotificar(entrega);
    }

    @Override
    @Transactional
    public EntregaModel confirmarEntrega(Long entregaId) {

        EntregaModel entrega = obtenerEntrega(entregaId);

        if (!"EnCamino".equals(entrega.getEstado())) {
            throw new IllegalStateException(
                    "La entrega debe estar en camino");
        }

        pedidoService.marcarPedidoEntregado(entrega.getPedido().getId());

        entrega.setEstado("Entregada");
        entrega.setFechaEntrega(LocalDateTime.now());

        RepartidorModel repartidor = entrega.getRepartidor();
        repartidor.setDisponible(true);
        repartidorRepository.save(repartidor);

        return guardarYNotificar(entrega);
    }

    @Override
    @Transactional
    public int reintentarAsignacionesPendientes() {

        int asignadas = 0;

        for (EntregaModel entrega :
                entregaRepository.findByEstado("PendienteAsignacion")) {

            try {
                asignarRepartidor(entrega.getId());
                asignadas++;
            } catch (IllegalStateException e) {
                continue;
            }
        }

        return asignadas;
    }

    private EntregaModel asignar(
            EntregaModel entrega, RepartidorModel repartidor) {

        if (!repartidor.isActivo() || !repartidor.isDisponible()) {
            throw new IllegalStateException(
                    "El repartidor no está activo o disponible");
        }

        repartidor.setDisponible(false);
        repartidorRepository.save(repartidor);

        entrega.setRepartidor(repartidor);
        entrega.setEstado("Asignada");
        entrega.setFechaAsignacion(LocalDateTime.now());
        entrega.setLatitudActual(repartidor.getLatitud());
        entrega.setLongitudActual(repartidor.getLongitud());

        return guardarYNotificar(entrega);
    }

    private EntregaModel guardarYNotificar(EntregaModel entrega) {

        EntregaModel guardada = entregaRepository.save(entrega);

        RepartidorModel repartidor = guardada.getRepartidor();

        eventos.publishEvent(new EntregaActualizadaEvent(
                guardada.getId(),
                guardada.getPedido().getId(),
                guardada.getEstado(),
                repartidor != null ? repartidor.getId() : null,
                guardada.getLatitudActual(),
                guardada.getLongitudActual(),
                LocalDateTime.now()));

        return guardada;
    }

    private EntregaModel obtenerEntrega(Long id) {
        return entregaRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "La entrega no existe"));
    }

    private void validarPendienteAsignacion(EntregaModel entrega) {
        if (!"PendienteAsignacion".equals(entrega.getEstado())) {
            throw new IllegalStateException(
                    "La entrega no está pendiente de asignación");
        }
    }

    private void validarCoordenadas(Double latitud, Double longitud) {
        if (latitud == null || longitud == null ||
                !Double.isFinite(latitud) ||
                !Double.isFinite(longitud) ||
                latitud < -90 || latitud > 90 ||
                longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException(
                    "Las coordenadas no son válidas");
        }
    }
}
