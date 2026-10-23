package com.api.rapi.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.rapi.events.FacturaSolicitadaEvent;
import com.api.rapi.exceptions.RecursoNoEncontradoException;
import com.api.rapi.integration.facturacion.ResultadoFacturacion;
import com.api.rapi.integration.facturacion.SistemaFacturacion;
import com.api.rapi.models.FacturaModel;
import com.api.rapi.models.PedidoModel;
import com.api.rapi.repositories.FacturaRepository;
import com.api.rapi.repositories.PagoRepository;
import com.api.rapi.repositories.PedidoRepository;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final SistemaFacturacion sistemaFacturacion;
    private final ApplicationEventPublisher eventos;

    public FacturaServiceImpl(
            FacturaRepository facturaRepository,
            PedidoRepository pedidoRepository,
            PagoRepository pagoRepository,
            SistemaFacturacion sistemaFacturacion,
            ApplicationEventPublisher eventos) {

        this.facturaRepository = facturaRepository;
        this.pedidoRepository = pedidoRepository;
        this.pagoRepository = pagoRepository;
        this.sistemaFacturacion = sistemaFacturacion;
        this.eventos = eventos;
    }

    @Override
    public List<FacturaModel> obtenerFacturas() {
        return facturaRepository.findAll();
    }

    @Override
    public Optional<FacturaModel> buscarFacturaPorId(Long id) {
        return facturaRepository.findById(id);
    }

    @Override
    public Optional<FacturaModel> buscarFacturaPorPedido(Long pedidoId) {
        return facturaRepository.findByPedidoId(pedidoId);
    }

    @Override
    public Optional<FacturaModel> buscarFacturaPorFolioFiscal(
            String folioFiscal) {

        return facturaRepository.findByFolioFiscal(folioFiscal);
    }

    @Override
    public List<FacturaModel> buscarFacturasPorEstado(String estado) {
        return facturaRepository.findByEstado(estado);
    }

    @Override
    public List<FacturaModel> buscarFacturasPorRFC(String rfcReceptor) {
        return facturaRepository.findByRfcReceptor(rfcReceptor);
    }

    @Override
    @Transactional
    public FacturaModel solicitarFactura(
            Long pedidoId,
            String rfcReceptor,
            String razonSocial,
            String correoFacturacion) {

        PedidoModel pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El pedido no existe"));

        if (facturaRepository.existsByPedidoId(pedidoId)) {
            throw new IllegalStateException(
                    "El pedido ya tiene una factura solicitada");
        }

        if (rfcReceptor == null || rfcReceptor.isBlank()) {
            throw new IllegalArgumentException(
                    "El RFC del receptor es obligatorio");
        }

        if (razonSocial == null || razonSocial.isBlank()) {
            throw new IllegalArgumentException(
                    "La razón social es obligatoria");
        }

        if (correoFacturacion == null ||
                correoFacturacion.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo de facturación es obligatorio");
        }

        if ("Cancelado".equals(pedido.getEstado()) ||
                "Rechazado".equals(pedido.getEstado())) {
            throw new IllegalStateException(
                    "No se pueden facturar pedidos cancelados o rechazados");
        }

        boolean pagoConfirmado = pagoRepository
                .existsByPedidoIdAndEstado(pedidoId, "Confirmado");

        if (!pagoConfirmado) {
            throw new IllegalStateException(
                    "El pedido debe tener un pago confirmado");
        }

        if (pedido.getTotal() == null ||
                pedido.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "El pedido debe tener un total válido");
        }

        FacturaModel factura = new FacturaModel();

        factura.setPedido(pedido);
        factura.setRfcReceptor(rfcReceptor.trim().toUpperCase());
        factura.setRazonSocial(razonSocial.trim());
        factura.setCorreoFacturacion(correoFacturacion.trim());
        factura.setMontoTotal(pedido.getTotal());
        factura.setEstado("Solicitada");
        factura.setFechaSolicitud(LocalDateTime.now());

        FacturaModel guardada = facturaRepository.save(factura);

        eventos.publishEvent(new FacturaSolicitadaEvent(guardada.getId()));

        return guardada;
    }

    @Override
    @Transactional
    public FacturaModel procesarFactura(Long facturaId) {

        FacturaModel factura = obtenerFactura(facturaId);

        if (!"Solicitada".equals(factura.getEstado())) {
            throw new IllegalStateException(
                    "La factura no está pendiente de procesamiento");
        }

        factura.setEstado("EnProceso");
        facturaRepository.saveAndFlush(factura);

        ResultadoFacturacion resultado =
                sistemaFacturacion.emitirFactura(factura);

        if (!resultado.exito()) {
            factura.setEstado("Error");
            return facturaRepository.save(factura);
        }

        factura.setFolioFiscal(resultado.folioFiscal());
        factura.setReferenciaExterna(resultado.referencia());
        factura.setFechaEmision(LocalDateTime.now());
        factura.setEstado("Emitida");

        return facturaRepository.save(factura);
    }

    @Override
    @Transactional
    public FacturaModel reintentarFactura(Long facturaId) {

        FacturaModel factura = obtenerFactura(facturaId);

        if (!"Error".equals(factura.getEstado())) {
            throw new IllegalStateException(
                    "Solo se pueden reintentar facturas con error");
        }

        factura.setEstado("Solicitada");

        FacturaModel guardada = facturaRepository.save(factura);

        eventos.publishEvent(new FacturaSolicitadaEvent(guardada.getId()));

        return guardada;
    }

    private FacturaModel obtenerFactura(Long id) {

        return facturaRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "La factura no existe"));
    }
}
