package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.ProductoRequest;
import com.api.rapi.models.ProductoModel;
import com.api.rapi.services.ProductoService;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoModel> obtenerProductos() {
        return productoService.obtenerProductos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoModel> obtenerProductoPorId(
            @PathVariable Long id) {

        return productoService.buscarProductoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurante/{restauranteId}")
    public List<ProductoModel> obtenerProductosPorRestaurante(
            @PathVariable Long restauranteId) {

        return productoService.buscarProductosPorRestaurante(
                restauranteId);
    }

    @GetMapping("/restaurante/{restauranteId}/disponibles")
    public List<ProductoModel> obtenerProductosDisponibles(
            @PathVariable Long restauranteId) {

        return productoService.obtenerProductosDisponibles(
                restauranteId);
    }

    @GetMapping("/buscar")
    public List<ProductoModel> buscarProductosPorNombre(
            @RequestParam String nombre) {

        return productoService.buscarProductosPorNombre(nombre);
    }

    @GetMapping("/buscar/restaurante/{restauranteId}")
    public List<ProductoModel> buscarPorNombreYRestaurante(
            @PathVariable Long restauranteId,
            @RequestParam String nombre) {

        return productoService.buscarProductosPorNombreYRestaurante(
                restauranteId, nombre);
    }

    @PostMapping
    public ResponseEntity<ProductoModel> registrarProducto(
            @RequestBody ProductoRequest solicitud) {

        ProductoModel nuevoProducto = productoService.registrarProducto(
                solicitud.restauranteId(), aModelo(solicitud));

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProducto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoModel> actualizarProducto(
            @PathVariable Long id,
            @RequestBody ProductoRequest solicitud) {

        ProductoModel actualizado =
                productoService.actualizarProducto(id, aModelo(solicitud));

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<ProductoModel> cambiarDisponibilidad(
            @PathVariable Long id,
            @RequestParam boolean disponible) {

        ProductoModel actualizado =
                productoService.cambiarDisponibilidad(id, disponible);

        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id) {

        productoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }

    private ProductoModel aModelo(ProductoRequest solicitud) {

        ProductoModel producto = new ProductoModel();

        producto.setNombre(solicitud.nombre());
        producto.setDescripcion(solicitud.descripcion());
        producto.setPrecio(solicitud.precio());
        producto.setDisponible(!Boolean.FALSE.equals(solicitud.disponible()));

        return producto;
    }
}
