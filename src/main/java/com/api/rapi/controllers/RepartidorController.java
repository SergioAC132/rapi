package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.dto.UbicacionRequest;
import com.api.rapi.models.RepartidorModel;
import com.api.rapi.services.RepartidorService;

@RestController
@RequestMapping("/api/repartidores")
public class RepartidorController {

    private final RepartidorService repartidorService;

    public RepartidorController(RepartidorService repartidorService) {
        this.repartidorService = repartidorService;
    }

    @GetMapping
    public List<RepartidorModel> obtenerRepartidores() {
        return repartidorService.obtenerRepartidores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepartidorModel> obtenerRepartidorPorId(
            @PathVariable Long id) {

        return repartidorService.buscarRepartidorPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activos")
    public List<RepartidorModel> obtenerRepartidoresActivos() {
        return repartidorService.obtenerRepartidoresActivos();
    }

    @GetMapping("/disponibles")
    public List<RepartidorModel> obtenerRepartidoresDisponibles() {
        return repartidorService.obtenerRepartidoresDisponibles();
    }

    @GetMapping("/buscar")
    public List<RepartidorModel> buscarPorNombre(
            @RequestParam String nombre) {

        return repartidorService.buscarRepartidoresPorNombre(nombre);
    }

    @GetMapping("/vehiculo")
    public List<RepartidorModel> buscarPorTipoVehiculo(
            @RequestParam String tipoVehiculo) {

        return repartidorService.buscarRepartidoresPorTipoVehiculo(
                tipoVehiculo);
    }

    @PostMapping
    public ResponseEntity<RepartidorModel> registrarRepartidor(
            @RequestBody RepartidorModel repartidor) {

        RepartidorModel nuevoRepartidor =
                repartidorService.registrarRepartidor(repartidor);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRepartidor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepartidorModel> actualizarRepartidor(
            @PathVariable Long id,
            @RequestBody RepartidorModel repartidor) {

        RepartidorModel actualizado =
                repartidorService.actualizarRepartidor(id, repartidor);

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<RepartidorModel> cambiarDisponibilidad(
            @PathVariable Long id,
            @RequestParam boolean disponible) {

        RepartidorModel actualizado =
                repartidorService.cambiarDisponibilidad(id, disponible);

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<RepartidorModel> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo) {

        RepartidorModel actualizado =
                repartidorService.cambiarEstado(id, activo);

        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/ubicacion")
    public ResponseEntity<RepartidorModel> actualizarUbicacion(
            @PathVariable Long id,
            @RequestBody UbicacionRequest ubicacion) {

        RepartidorModel actualizado =
                repartidorService.actualizarUbicacion(
                        id, ubicacion.latitud(), ubicacion.longitud());

        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/mas-cercano")
    public ResponseEntity<RepartidorModel> buscarRepartidorMasCercano(
            @RequestParam Double latitud,
            @RequestParam Double longitud) {

        return repartidorService.buscarRepartidorMasCercano(
                        latitud, longitud)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRepartidor(
            @PathVariable Long id) {

        repartidorService.eliminarRepartidor(id);

        return ResponseEntity.noContent().build();
    }
}
