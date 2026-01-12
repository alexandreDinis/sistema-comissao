package com.empresa.comissao.controller;

import com.empresa.comissao.dto.request.ClienteRequest;
import com.empresa.comissao.dto.response.ClienteResponse;
import com.empresa.comissao.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de Clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Criar cliente")
    public ResponseEntity<ClienteResponse> criar(
            @Valid @RequestBody ClienteRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        return ResponseEntity.ok(clienteService.criar(request, usuario));
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Listagem com suporte a filtros")
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) com.empresa.comissao.domain.enums.StatusCliente status) {
        return ResponseEntity.ok(clienteService.listar(termo, cidade, bairro, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar por ID")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar cliente")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
