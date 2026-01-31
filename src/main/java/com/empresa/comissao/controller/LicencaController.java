package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.service.LicencaService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/licencas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class LicencaController {

    private final LicencaService licencaService;

    @PostMapping
    public ResponseEntity<Licenca> criarLicenca(@RequestBody CriarLicencaRequest request, @RequestParam Long planoId) {
        Licenca licenca = Licenca.builder()
                .razaoSocial(request.getRazaoSocial())
                .nomeFantasia(request.getNomeFantasia())
                .cnpj(request.getCnpj())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .build();

        return ResponseEntity.ok(licencaService.criarLicenca(licenca, planoId, request.getSenhaAdmin()));
    }

    @GetMapping
    public ResponseEntity<Page<Licenca>> listarLicencas(Pageable pageable) {
        return ResponseEntity.ok(licencaService.listarLicencas(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Licenca> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(licencaService.buscarPorId(id));
    }

    @PostMapping("/{id}/suspender")
    public ResponseEntity<Void> suspenderLicenca(@PathVariable Long id, @RequestBody String motivo) {
        licencaService.suspenderLicenca(id, motivo);
        return ResponseEntity.ok().build();
    }

    @Data
    static class CriarLicencaRequest {
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
        private String email;
        private String telefone;
        private String senhaAdmin;
    }

    @Data
    public static class AtualizarLicencaRequest {
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
        private String email;
        private String telefone;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Licenca> atualizarLicenca(@PathVariable Long id,
            @RequestBody AtualizarLicencaRequest request) {
        return ResponseEntity.ok(licencaService.atualizarLicenca(id, request));
    }
}
