package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.PlanoLicenca;
import com.empresa.comissao.repository.PlanoLicencaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/planos-licenca")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlanoLicencaController {

    private final PlanoLicencaRepository planoLicencaRepository;

    @GetMapping
    public ResponseEntity<List<PlanoLicenca>> listar() {
        return ResponseEntity.ok(planoLicencaRepository.findByAtivoTrueOrderByOrdemAsc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanoLicenca> buscarPorId(@PathVariable Long id) {
        return planoLicencaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlanoLicenca> criar(@RequestBody PlanoLicenca plano) {
        plano.setId(null); // Force new creation
        return ResponseEntity.ok(planoLicencaRepository.save(plano));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanoLicenca> atualizar(@PathVariable Long id, @RequestBody PlanoLicenca planoAtualizado) {
        return planoLicencaRepository.findById(id)
                .map(plano -> {
                    plano.setNome(planoAtualizado.getNome());
                    plano.setDescricao(planoAtualizado.getDescricao());
                    plano.setValorMensalidade(planoAtualizado.getValorMensalidade());
                    plano.setValorPorTenant(planoAtualizado.getValorPorTenant());
                    plano.setLimiteTenants(planoAtualizado.getLimiteTenants());
                    plano.setLimiteUsuariosPorTenant(planoAtualizado.getLimiteUsuariosPorTenant());
                    plano.setSuportePrioritario(planoAtualizado.isSuportePrioritario());
                    plano.setWhiteLabel(planoAtualizado.isWhiteLabel());
                    plano.setDominioCustomizado(planoAtualizado.isDominioCustomizado());
                    plano.setOrdem(planoAtualizado.getOrdem());
                    return ResponseEntity.ok(planoLicencaRepository.save(plano));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        return planoLicencaRepository.findById(id)
                .map(plano -> {
                    plano.setAtivo(false); // Soft delete
                    planoLicencaRepository.save(plano);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
