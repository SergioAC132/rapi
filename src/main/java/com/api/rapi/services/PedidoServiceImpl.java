package com.api.rapi.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.rapi.events.PedidoAceptadoEvent;
import com.api.rapi.events.PedidoCanceladoEvent;
import com.api.rapi.exceptions.RecursoNoEncontradoException;
import com.api.rapi.models.ClienteModel;
import com.api.rapi.models.PedidoModel;
import com.api.rapi.models.RestauranteModel;

import com.api.rapi.repositories.ClienteRepository;
import com.api.rapi.repositories.PedidoRepository;
import com.api.rapi.repositories.RestauranteRepository;

@Service
public class PedidoServiceImpl implements PedidoService {

    private static final long MINUTOS_PARA_ACEPTAR = 5;

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final ApplicationEventPublisher eventos;

    public PedidoServiceImpl(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            RestauranteRepository restauranteRepository,
            ApplicationEventPublisher eventos) {

        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.eventos = eventos;
    }

    @Override
    public List<PedidoModel> obtenerPedidos() {
        return pedidoRepository.findAll();
    }

    @Override
    public Optional<PedidoModel> buscarPedidoPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    @Override
    public List<PedidoModel> buscarPedidosPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    @Override
    public List<PedidoModel> buscarPedidosPorRestaurante(Long restauranteId) {
        return pedidoRepository.findByRestauranteId(restauranteId);
    }

    @Override
    public List<PedidoModel> buscarPedidosPorEstado(String estado) {
        return pedidoRepository.findByEstado(estado);
    }

    @Override
    public List<PedidoModel> buscarPedidosPorRestauranteYEstado(
            Long restauranteId, String estado) {

        return pedidoRepository.findByRestauranteIdAndEstado(
                restauranteId, estado);
    }

    @Override
    @Transactional
    public PedidoModel crearPedido(
            Long clienteId,
            Long restauranteId,
            String direccionEntrega) {

        if (clienteId == null || restauranteId == null) {
            throw new IllegalArgumentException(
                    "El cliente y el restaurante son obligatorios");
        }

        ClienteModel cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El cliente no existe"));

        RestauranteModel restaurante = restauranteRepository
                .findById(restauranteId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El restaurante no existe"));

        if (!restaurante.isActivo() || !restaurante.isAbierto()) {
            throw new IllegalStateException(
                    "El restaurante no está disponible");
        }

        if (direccionEntrega == null ||
                direccionEntrega.isBlank()) {
            throw new IllegalArgumentException(
                    "La dirección de entrega es obligatoria");
        }

        PedidoModel pedido = new PedidoModel();

        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setDireccionEntrega(direccionEntrega.trim());
        pedido.setTotal(BigDecimal.ZERO);
        pedido.setEstado("PendientePago");

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public PedidoModel confirmarPagoPedido(Long pedidoId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        if (!"PendientePago".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido no está pendiente de pago");
        }

        pedido.setEstado("PendienteAceptacion");
        pedido.setFechaLimiteAceptacion(
                LocalDateTime.now().plusMinutes(MINUTOS_PARA_ACEPTAR));

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public PedidoModel aceptarPedido(
            Long pedidoId, Long restauranteId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        validarRestaurantePedido(pedido, restauranteId);

        if (!"PendienteAceptacion".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido no está pendiente de aceptación");
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (!ahora.isBefore(pedido.getFechaLimiteAceptacion())) {
            return cancelar(pedido, "Cancelado",
                    "El restaurante no aceptó a tiempo");
        }

        pedido.setEstado("Aceptado");
        pedido.setFechaAceptacion(ahora);

        PedidoModel aceptado = pedidoRepository.save(pedido);

        eventos.publishEvent(new PedidoAceptadoEvent(aceptado.getId()));

        return aceptado;
    }

    @Override
    @Transactional
    public PedidoModel rechazarPedido(
            Long pedidoId, Long restauranteId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        validarRestaurantePedido(pedido, restauranteId);

        if (!"PendienteAceptacion".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "Solo se pueden rechazar pedidos pendientes de aceptación");
        }

        return cancelar(pedido, "Rechazado",
                "El restaurante rechazó el pedido");
    }

    @Override
    @Transactional
    public PedidoModel cambiarEstadoPedido(
            Long pedidoId, String nuevoEstado) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        if ("Aceptado".equals(pedido.getEstado()) &&
                "EnPreparacion".equals(nuevoEstado)) {

            pedido.setEstado("EnPreparacion");

            return pedidoRepository.save(pedido);
        }

        throw new IllegalStateException(
                "No está permitida esta transición de estado");
    }

    @Override
    @Transactional
    public PedidoModel marcarPedidoEnCamino(Long pedidoId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        if (!"Aceptado".equals(pedido.getEstado()) &&
                !"EnPreparacion".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido debe estar aceptado o en preparación");
        }

        pedido.setEstado("EnCamino");

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public PedidoModel marcarPedidoEntregado(Long pedidoId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        if (!"EnCamino".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido debe estar en camino");
        }

        pedido.setEstado("Entregado");
        pedido.setFechaEntrega(LocalDateTime.now());

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public PedidoModel cancelarPedido(Long pedidoId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        if ("PendientePago".equals(pedido.getEstado())) {
            pedido.setEstado("Cancelado");
            return pedidoRepository.save(pedido);
        }

        if ("PendienteAceptacion".equals(pedido.getEstado())) {
            return cancelar(pedido, "Cancelado",
                    "El cliente canceló el pedido");
        }

        throw new IllegalStateException(
                "No se puede cancelar un pedido en su estado actual");
    }

    @Override
    public List<PedidoModel> obtenerPedidosConAceptacionVencida(
            LocalDateTime fechaActual) {

        if (fechaActual == null) {
            throw new IllegalArgumentException(
                    "La fecha de consulta es obligatoria");
        }

        return pedidoRepository
                .findByEstadoAndFechaLimiteAceptacionBefore(
                        "PendienteAceptacion",
                        fechaActual.plusNanos(1));
    }

    @Override
    @Transactional
    public int cancelarPedidosVencidos() {

        List<PedidoModel> vencidos =
                obtenerPedidosConAceptacionVencida(LocalDateTime.now());

        for (PedidoModel pedido : vencidos) {
            cancelar(pedido, "Cancelado",
                    "El restaurante no aceptó a tiempo");
        }

        return vencidos.size();
    }

    private PedidoModel obtenerPedido(Long id) {

        return pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El pedido no existe"));
    }

    private void validarRestaurantePedido(
            PedidoModel pedido, Long restauranteId) {

        if (restauranteId == null ||
                !pedido.getRestaurante().getId().equals(restauranteId)) {

            throw new IllegalArgumentException(
                    "El pedido no pertenece a este restaurante");
        }
    }

    private PedidoModel cancelar(
            PedidoModel pedido, String estadoFinal, String motivo) {

        pedido.setEstado(estadoFinal);

        PedidoModel cancelado = pedidoRepository.save(pedido);

        eventos.publishEvent(
                new PedidoCanceladoEvent(cancelado.getId(), motivo));

        return cancelado;
    }
}
