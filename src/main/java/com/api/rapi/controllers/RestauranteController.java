package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.models.RestauranteModel;
import com.api.rapi.services.RestauranteService;

@RestController
@RequestMapping("/api/restaurantes")
public class RestauranteController {

    private final RestauranteService restauranteService;

    public RestauranteController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @GetMapping
    public List<RestauranteModel> obtenerRestaurantes() {
        return restauranteService.obtenerRestaurantes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestauranteModel> obtenerRestaurantePorId(
            @PathVariable Long id) {

        return restauranteService.buscarRestaurantePorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public List<RestauranteModel> buscarPorNombre(
            @RequestParam String nombre) {

        return restauranteService.buscarRestaurantesPorNombre(nombre);
    }

    @GetMapping("/tipo-comida")
    public List<RestauranteModel> buscarPorTipoComida(
            @RequestParam String tipoComida) {

        return restauranteService.buscarRestaurantesPorTipoComida(tipoComida);
    }

    @GetMapping("/activos")
    public List<RestauranteModel> obtenerRestaurantesActivos() {
        return restauranteService.obtenerRestaurantesActivos();
    }

    @GetMapping("/abiertos")
    public List<RestauranteModel> obtenerRestaurantesAbiertos() {
        return restauranteService.obtenerRestaurantesAbiertos();
    }

    @PostMapping
    public ResponseEntity<RestauranteModel> registrarRestaurante(
            @RequestBody RestauranteModel restaurante) {

        RestauranteModel nuevoRestaurante =
                restauranteService.registrarRestaurante(restaurante);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRestaurante);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestauranteModel> actualizarRestaurante(
            @PathVariable Long id,
            @RequestBody RestauranteModel restaurante) {

        RestauranteModel actualizado =
                restauranteService.actualizarRestaurante(id, restaurante);

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<RestauranteModel> cambiarDisponibilidad(
            @PathVariable Long id,
            @RequestParam boolean abierto) {

        RestauranteModel actualizado =
                restauranteService.cambiarDisponibilidad(id, abierto);

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<RestauranteModel> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {

        RestauranteModel actualizado =
                restauranteService.cambiarEstado(id, activo);

        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRestaurante(
            @PathVariable Long id) {

        restauranteService.eliminarRestaurante(id);

        return ResponseEntity.noContent().build();
    }
}
