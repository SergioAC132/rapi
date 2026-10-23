package com.api.rapi.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.rapi.models.ClienteModel;
import com.api.rapi.services.ClienteService;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteModel> obtenerClientes() {
        return clienteService.obtenerClientes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteModel> obtenerClientePorId(
            @PathVariable Long id) {

        return clienteService.buscarClientePorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/correo")
    public ResponseEntity<ClienteModel> buscarPorCorreo(
            @RequestParam String correo) {

        return clienteService.buscarClientePorCorreo(correo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public List<ClienteModel> buscarPorNombre(
            @RequestParam String nombre) {

        return clienteService.buscarClientesPorNombre(nombre);
    }

    @PostMapping
    public ResponseEntity<ClienteModel> registrarCliente(
            @RequestBody ClienteModel cliente) {

        ClienteModel nuevoCliente =
                clienteService.registrarCliente(cliente);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteModel> actualizarCliente(
            @PathVariable Long id,
            @RequestBody ClienteModel cliente) {

        ClienteModel actualizado =
                clienteService.actualizarCliente(id, cliente);

        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(
            @PathVariable Long id) {

        clienteService.eliminarCliente(id);

        return ResponseEntity.noContent().build();
    }
}
