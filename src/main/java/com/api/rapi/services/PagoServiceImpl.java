package com.api.rapi.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.rapi.exceptions.RecursoNoEncontradoException;
import com.api.rapi.integration.pagos.PasarelaPagos;
import com.api.rapi.integration.pagos.ResultadoPasarela;
import com.api.rapi.models.PagoModel;
import com.api.rapi.models.PedidoModel;
import com.api.rapi.repositories.PagoRepository;
import com.api.rapi.repositories.PedidoRepository;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final PasarelaPagos pasarelaPagos;

    public PagoServiceImpl(
            PagoRepository pagoRepository,
            PedidoRepository pedidoRepository,
            PedidoService pedidoService,
            PasarelaPagos pasarelaPagos) {

        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
        this.pasarelaPagos = pasarelaPagos;
    }

    @Override
    public List<PagoModel> obtenerPagos() {
        return pagoRepository.findAll();
    }

    @Override
    public Optional<PagoModel> buscarPagoPorId(Long id) {
        return pagoRepository.findById(id);
    }

    @Override
    public Optional<PagoModel> buscarPagoPorPedido(Long pedidoId) {
        return pagoRepository.findByPedidoId(pedidoId);
    }

    @Override
    public Optional<PagoModel> buscarPagoPorReferencia(
            String referenciaExterna) {

        return pagoRepository.findByReferenciaExterna(
                referenciaExterna);
    }

    @Override
    public List<PagoModel> buscarPagosPorEstado(String estado) {
        return pagoRepository.findByEstado(estado);
    }

    @Override
    @Transactional
    public PagoModel procesarPago(Long pedidoId, String tokenTarjeta) {

        if (tokenTarjeta == null || tokenTarjeta.isBlank()) {
            throw new IllegalArgumentException(
                    "El token de la tarjeta es obligatorio");
        }

        PedidoModel pedido = obtenerPedido(pedidoId);

        if (!"PendientePago".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido no está pendiente de pago");
        }

        if (pedido.getTotal() == null ||
                pedido.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "El pedido debe tener productos y un total válido");
        }

        PagoModel pago = pagoRepository.findByPedidoId(pedidoId)
                .orElseGet(PagoModel::new);

        if (pago.getId() != null && !"Fallido".equals(pago.getEstado())) {
            throw new IllegalStateException(
                    "El pedido ya tiene un pago registrado");
        }

        pago.setPedido(pedido);
        pago.setMonto(pedido.getTotal());
        pago.setMetodoPago("Tarjeta");

        ResultadoPasarela resultado = pasarelaPagos.cobrar(
                pedidoId, pedido.getTotal(), tokenTarjeta.trim());

        if (!resultado.aprobado()) {
            pago.setEstado("Fallido");
            pago.setReferenciaExterna(null);
            pago.setFechaPago(null);
            return pagoRepository.save(pago);
        }

        pago.setEstado("Confirmado");
        pago.setReferenciaExterna(resultado.referencia());
        pago.setFechaPago(LocalDateTime.now());

        PagoModel guardado = pagoRepository.save(pago);

        pedidoService.confirmarPagoPedido(pedidoId);

        return guardado;
    }

    @Override
    @Transactional
    public PagoModel reembolsarPago(Long pedidoId) {

        PedidoModel pedido = obtenerPedido(pedidoId);

        if (!"Cancelado".equals(pedido.getEstado()) &&
                !"Rechazado".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "El pedido debe estar cancelado o rechazado");
        }

        PagoModel pago = pagoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe un pago para el pedido"));

        if ("Reembolsado".equals(pago.getEstado())) {
            return pago;
        }

        if (!"Confirmado".equals(pago.getEstado()) &&
                !"ReembolsoPendiente".equals(pago.getEstado())) {
            throw new IllegalStateException(
                    "El pago no permite solicitar un reembolso");
        }

        ResultadoPasarela resultado = pasarelaPagos.reembolsar(
                pago.getReferenciaExterna(), pago.getMonto());

        if (!resultado.aprobado()) {
            pago.setEstado("ReembolsoPendiente");
            return pagoRepository.save(pago);
        }

        pago.setEstado("Reembolsado");
        pago.setReferenciaReembolso(resultado.referencia());
        pago.setFechaReembolso(LocalDateTime.now());

        return pagoRepository.save(pago);
    }

    @Override
    public boolean verificarPagoConfirmado(Long pedidoId) {

        return pagoRepository.existsByPedidoIdAndEstado(
                pedidoId, "Confirmado");
    }

    private PedidoModel obtenerPedido(Long pedidoId) {

        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El pedido no existe"));
    }
}
