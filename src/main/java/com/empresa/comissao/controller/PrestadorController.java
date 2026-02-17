package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Prestador;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.PrestadorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciar Prestadores de Serviço Externos.
 * Cadastro simples: nome, telefone, chave PIX.
 */
@RestController
@RequestMapping("/api/v1/prestadores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prestadores", description = "Cadastro de prestadores de serviço externos")
public class PrestadorController {

    private final PrestadorRepository prestadorRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'FUNCIONARIO', 'USER')")
    @Operation(summary = "Listar prestadores ativos")
    public ResponseEntity<List<Prestador>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado no contexto");
        }
        Empresa empresa = new Empresa();
        empresa.setId(tenantId);

        List<Prestador> prestadores = apenasAtivos
                ? prestadorRepository.findByEmpresaAndAtivoTrueOrderByNomeAsc(empresa)
                : prestadorRepository.findByEmpresaOrderByNomeAsc(empresa);

        return ResponseEntity.ok(prestadores);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Cadastrar prestador", description = "Cadastro simples: nome, telefone, chave PIX")
    public ResponseEntity<Prestador> criar(
            @Valid @RequestBody PrestadorRequest request) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado no contexto");
        }
        Empresa empresa = new Empresa();
        empresa.setId(tenantId);

        Prestador prestador = Prestador.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .chavePix(request.chavePix())
                .ativo(true)
                .empresa(empresa)
                .build();

        Prestador salvo = prestadorRepository.save(prestador);
        log.info("✅ Prestador cadastrado: {} - Empresa ID: {}", salvo.getNome(), tenantId);

        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Atualizar prestador")
    public ResponseEntity<Prestador> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PrestadorRequest request) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado no contexto");
        }

        Prestador prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Prestador não encontrado"));

        // Verificar se pertence à empresa do usuário
        if (!prestador.getEmpresa().getId().equals(tenantId)) {
            throw new BusinessException("Prestador não pertence à sua empresa");
        }

        prestador.setNome(request.nome());
        prestador.setTelefone(request.telefone());
        prestador.setChavePix(request.chavePix());

        return ResponseEntity.ok(prestadorRepository.save(prestador));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Desativar prestador", description = "Soft delete - apenas marca como inativo")
    public ResponseEntity<Void> desativar(
            @PathVariable Long id) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado no contexto");
        }

        Prestador prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Prestador não encontrado"));

        if (!prestador.getEmpresa().getId().equals(tenantId)) {
            throw new BusinessException("Prestador não pertence à sua empresa");
        }

        prestador.setAtivo(false);
        prestadorRepository.save(prestador);

        log.info("🗑️ Prestador desativado: {}", prestador.getNome());
        return ResponseEntity.noContent().build();
    }

    // DTO Request
    public record PrestadorRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            String telefone,
            String chavePix) {
    }
}
