package com.api.rapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.api.rapi.models.PedidoModel;
import com.api.rapi.repositories.PedidoRepository;
import com.api.rapi.services.PedidoService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class FlujoPedidoTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoService pedidoService;

    private long restauranteId;
    private long productoId;
    private long clienteId;

    @BeforeEach
    void prepararCatalogo() throws Exception {

        restauranteId = crear("/api/restaurantes", """
                {"nombre":"Tacos Don Pepe","direccion":"Centro 10",
                 "tipoComida":"Mexicana","abierto":true,"activo":true,
                 "latitud":19.4326,"longitud":-99.1332}
                """);

        productoId = crear("/api/productos", """
                {"restauranteId":%d,"nombre":"Orden de pastor",
                 "precio":85.50,"disponible":true}
                """.formatted(restauranteId));

        clienteId = crear("/api/clientes", """
                {"nombre":"Ana","apellido":"López",
                 "correo":"ana%d@correo.com","direccion":"Roma 5"}
                """.formatted(System.nanoTime()));
    }

    @Test
    void flujoCompletoAsignaAlRepartidorMasCercanoYFactura() throws Exception {

        long lejano = crearRepartidor(19.60, -99.30);
        long cercano = crearRepartidor(19.4330, -99.1330);

        long pedidoId = crearPedidoConProducto();

        pagar(pedidoId, "tok_visa", 201, "Confirmado");

        mockMvc.perform(patch("/api/pedidos/{id}/aceptar", pedidoId)
                        .param("restauranteId", String.valueOf(restauranteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Aceptado"));

        esperarValor("/api/entregas/pedido/" + pedidoId,
                "$.estado", "Asignada");

        String entrega = leer("/api/entregas/pedido/" + pedidoId);
        long entregaId = id(entrega);

        assertThat(((Number) JsonPath.read(entrega, "$.repartidor.id"))
                .longValue()).isEqualTo(cercano).isNotEqualTo(lejano);

        String seguimiento = mockMvc.perform(
                        get("/api/entregas/pedido/{id}/seguimiento", pedidoId))
                .andExpect(request().asyncStarted())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(seguimiento)
                .contains("event:entrega")
                .contains("\"estado\":\"Asignada\"");

        mockMvc.perform(patch("/api/entregas/{id}/recogida", entregaId))
                .andExpect(jsonPath("$.estado").value("Recogida"));

        mockMvc.perform(patch("/api/entregas/{id}/iniciar", entregaId))
                .andExpect(jsonPath("$.estado").value("EnCamino"));

        mockMvc.perform(patch("/api/entregas/{id}/ubicacion", entregaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitud\":19.44,\"longitud\":-99.14}"))
                .andExpect(jsonPath("$.latitudActual").value(19.44));

        mockMvc.perform(patch("/api/entregas/{id}/confirmar", entregaId))
                .andExpect(jsonPath("$.estado").value("Entregada"));

        esperarValor("/api/pedidos/" + pedidoId, "$.estado", "Entregado");

        crear("/api/calificaciones", """
                {"pedidoId":%d,"puntuacionRestaurante":5,
                 "puntuacionRepartidor":4,"comentario":"Rápido"}
                """.formatted(pedidoId));

        mockMvc.perform(post("/api/facturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":%d,"rfcReceptor":"XAXX010101000",
                                 "razonSocial":"Ana López",
                                 "correoFacturacion":"ana@correo.com"}
                                """.formatted(pedidoId)))
                .andExpect(status().isAccepted());

        esperarValor("/api/facturas/pedido/" + pedidoId,
                "$.estado", "Emitida");
    }

    @Test
    void pagoRechazadoPermiteReintentar() throws Exception {

        long pedidoId = crearPedidoConProducto();

        pagar(pedidoId, "tok_rechazada", 402, "Fallido");
        pagar(pedidoId, "tok_visa", 201, "Confirmado");

        esperarValor("/api/pedidos/" + pedidoId,
                "$.estado", "PendienteAceptacion");
    }

    @Test
    void rechazoDelRestauranteReembolsaElPago() throws Exception {

        long pedidoId = crearPedidoConProducto();
        pagar(pedidoId, "tok_visa", 201, "Confirmado");

        mockMvc.perform(patch("/api/pedidos/{id}/rechazar", pedidoId)
                        .param("restauranteId", String.valueOf(restauranteId)))
                .andExpect(jsonPath("$.estado").value("Rechazado"));

        esperarValor("/api/pagos/pedido/" + pedidoId,
                "$.estado", "Reembolsado");
    }

    @Test
    void pedidoNoAceptadoEnCincoMinutosSeCancelaYReembolsa() throws Exception {

        long pedidoId = crearPedidoConProducto();
        pagar(pedidoId, "tok_visa", 201, "Confirmado");

        PedidoModel pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        assertThat(pedido.getFechaLimiteAceptacion())
                .isAfter(LocalDateTime.now().plusMinutes(4));

        pedido.setFechaLimiteAceptacion(LocalDateTime.now().minusSeconds(1));
        pedidoRepository.save(pedido);

        pedidoService.cancelarPedidosVencidos();

        esperarValor("/api/pedidos/" + pedidoId, "$.estado", "Cancelado");
        esperarValor("/api/pagos/pedido/" + pedidoId,
                "$.estado", "Reembolsado");

        mockMvc.perform(patch("/api/pedidos/{id}/aceptar", pedidoId)
                        .param("restauranteId", String.valueOf(restauranteId)))
                .andExpect(status().isConflict());
    }

    @Test
    void erroresDeNegocioRegresanCodigosHttpAdecuados() throws Exception {

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clienteId":999999,"restauranteId":%d,
                                 "direccionEntrega":"Roma 5"}
                                """.formatted(restauranteId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("El cliente no existe"));

        long pedidoId = crear("/api/pedidos", """
                {"clienteId":%d,"restauranteId":%d,"direccionEntrega":"Roma 5"}
                """.formatted(clienteId, restauranteId));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":%d,\"tokenTarjeta\":\"tok_visa\"}"
                                .formatted(pedidoId)))
                .andExpect(status().isConflict());
    }

    private long crearPedidoConProducto() throws Exception {

        long pedidoId = crear("/api/pedidos", """
                {"clienteId":%d,"restauranteId":%d,"direccionEntrega":"Roma 5"}
                """.formatted(clienteId, restauranteId));

        crear("/api/detalles-pedido", """
                {"pedidoId":%d,"productoId":%d,"cantidad":2}
                """.formatted(pedidoId, productoId));

        return pedidoId;
    }

    private long crearRepartidor(double latitud, double longitud)
            throws Exception {

        return crear("/api/repartidores", """
                {"nombre":"Luis","tipoVehiculo":"Moto",
                 "disponible":true,"activo":true,
                 "latitud":%s,"longitud":%s}
                """.formatted(latitud, longitud));
    }

    private void pagar(long pedidoId, String token,
                       int estadoHttp, String estadoPago) throws Exception {

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":%d,\"tokenTarjeta\":\"%s\"}"
                                .formatted(pedidoId, token)))
                .andExpect(status().is(estadoHttp))
                .andExpect(jsonPath("$.estado").value(estadoPago));
    }

    private long crear(String url, String json) throws Exception {

        String respuesta = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return id(respuesta);
    }

    private String leer(String url) throws Exception {

        return mockMvc.perform(get(url))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private void esperarValor(String url, String ruta, Object esperado)
            throws Exception {

        Object actual = null;

        for (int intento = 0; intento < 50; intento++) {
            String respuesta = leer(url);

            if (!respuesta.isBlank()) {
                actual = JsonPath.read(respuesta, ruta);

                if (esperado.equals(actual)) {
                    return;
                }
            }

            Thread.sleep(100);
        }

        assertThat(actual).as(url + " " + ruta).isEqualTo(esperado);
    }

    private long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
