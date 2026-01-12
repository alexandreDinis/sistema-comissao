package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.ModoComissao;
import com.empresa.comissao.repository.EmpresaRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    /**
     * Get current empresa configuration for the authenticated user's company.
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<EmpresaConfigResponse> getConfig(@AuthenticationPrincipal User user) {
        if (user.getEmpresa() == null) {
            return ResponseEntity.badRequest().build();
        }

        Empresa empresa = user.getEmpresa();
        return ResponseEntity.ok(new EmpresaConfigResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getModoComissao()));
    }

    /**
     * Update empresa configuration (modoComissao).
     * Only ADMIN_EMPRESA can change their company's settings.
     */
    @PatchMapping("/config")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<EmpresaConfigResponse> updateConfig(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateEmpresaConfigRequest request) {

        if (user.getEmpresa() == null) {
            return ResponseEntity.badRequest().build();
        }

        Empresa empresa = empresaRepository.findById(user.getEmpresa().getId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        if (request.getModoComissao() != null) {
            empresa.setModoComissao(request.getModoComissao());
        }

        Empresa saved = empresaRepository.save(empresa);

        return ResponseEntity.ok(new EmpresaConfigResponse(
                saved.getId(),
                saved.getNome(),
                saved.getModoComissao()));
    }

    // DTOs
    @Data
    public static class EmpresaConfigResponse {
        private final Long id;
        private final String nome;
        private final ModoComissao modoComissao;
    }

    @Data
    public static class UpdateEmpresaConfigRequest {
        private ModoComissao modoComissao;
    }
}
