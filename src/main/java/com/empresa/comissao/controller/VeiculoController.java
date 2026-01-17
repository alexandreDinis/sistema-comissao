package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.dto.response.VeiculoHistoricoResponse;
import com.empresa.comissao.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/veiculos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Veículos", description = "Gestão de Veículos e Histórico")
public class VeiculoController {

    private final VeiculoService veiculoService;

    @GetMapping("/verificar-placa")
    @Operation(summary = "Verificar existência de placa", description = "Retorna se a placa já existe para a empresa do usuário logado.")
    public ResponseEntity<Map<String, Object>> verificarPlaca(
            @RequestParam String placa,
            @AuthenticationPrincipal User principal) {
        if (principal.getEmpresa() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuário não vinculado a uma empresa"));
        }
        return ResponseEntity.ok(veiculoService.verificarPlaca(placa, principal.getEmpresa()));
    }

    @GetMapping("/{placa}/historico")
    @Operation(summary = "Obter histórico do veículo", description = "Lista todas as OSs relacionadas a uma placa na empresa do usuário logado.")
    public ResponseEntity<List<VeiculoHistoricoResponse>> obterHistorico(
            @PathVariable String placa,
            @AuthenticationPrincipal User principal) {
        if (principal.getEmpresa() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(veiculoService.obterHistorico(placa, principal.getEmpresa()));
    }
}
