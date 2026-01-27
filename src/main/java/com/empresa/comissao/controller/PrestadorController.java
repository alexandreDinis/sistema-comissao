package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Prestador;
import com.empresa.comissao.domain.entity.User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Listar prestadores ativos")
    public ResponseEntity<List<Prestador>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        List<Prestador> prestadores = apenasAtivos
                ? prestadorRepository.findByEmpresaAndAtivoTrueOrderByNomeAsc(empresa)
                : prestadorRepository.findByEmpresaOrderByNomeAsc(empresa);

        return ResponseEntity.ok(prestadores);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Cadastrar prestador", description = "Cadastro simples: nome, telefone, chave PIX")
    public ResponseEntity<Prestador> criar(
            @Valid @RequestBody PrestadorRequest request,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        Prestador prestador = Prestador.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .chavePix(request.chavePix())
                .ativo(true)
                .empresa(empresa)
                .build();

        Prestador salvo = prestadorRepository.save(prestador);
        log.info("✅ Prestador cadastrado: {} - Empresa: {}", salvo.getNome(), empresa.getNome());

        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Atualizar prestador")
    public ResponseEntity<Prestador> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PrestadorRequest request,
            @AuthenticationPrincipal User usuario) {

        Prestador prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Prestador não encontrado"));

        // Verificar se pertence à empresa do usuário
        if (!prestador.getEmpresa().getId().equals(usuario.getEmpresa().getId())) {
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
            @PathVariable Long id,
            @AuthenticationPrincipal User usuario) {

        Prestador prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Prestador não encontrado"));

        if (!prestador.getEmpresa().getId().equals(usuario.getEmpresa().getId())) {
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
