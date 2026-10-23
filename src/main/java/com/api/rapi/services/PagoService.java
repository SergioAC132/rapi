package com.api.rapi.services;

import java.util.List;
import java.util.Optional;

import com.api.rapi.models.PagoModel;

public interface PagoService {

    List<PagoModel> obtenerPagos();

    Optional<PagoModel> buscarPagoPorId(Long id);

    Optional<PagoModel> buscarPagoPorPedido(Long pedidoId);

    Optional<PagoModel> buscarPagoPorReferencia(String referenciaExterna);

    List<PagoModel> buscarPagosPorEstado(String estado);

    PagoModel procesarPago(Long pedidoId, String tokenTarjeta);

    PagoModel reembolsarPago(Long pedidoId);

    boolean verificarPagoConfirmado(Long pedidoId);

}
