package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.CartaoCredito;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.CartaoCreditoRepository;
import com.empresa.comissao.service.FaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cartoes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cartões de Crédito", description = "Gerenciamento de cartões corporativos")
public class CartaoController {

    private final CartaoCreditoRepository cartaoRepository;
    private final FaturaService faturaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Listar cartões", description = "Lista todos os cartões de crédito da empresa")
    public ResponseEntity<List<CartaoCredito>> listar() {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado");
        }
        Empresa empresa = new Empresa();
        empresa.setId(tenantId);

        return ResponseEntity.ok(cartaoRepository.findByEmpresaAndAtivoTrueOrderByNomeAsc(empresa));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar cartão", description = "Cadastra um novo cartão de crédito corporativo")
    public ResponseEntity<CartaoCredito> criar(
            @Valid @RequestBody CartaoCreditoRequest request) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado");
        }
        Empresa empresa = new Empresa();
        empresa.setId(tenantId);

        CartaoCredito cartao = CartaoCredito.builder()
                .nome(request.nome())
                .diaVencimento(request.diaVencimento())
                .diaFechamento(request.diaFechamento() != null ? request.diaFechamento() : 25)
                .limite(request.limite())
                .empresa(empresa)
                .ativo(true)
                .build();

        CartaoCredito salvo = cartaoRepository.save(cartao);
        log.info("Cartao criado: {} (vencimento dia {}, limite: R$ {}) - Tenant: {}",
                salvo.getNome(), salvo.getDiaVencimento(), salvo.getLimite(), tenantId);
        return ResponseEntity.ok(salvo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar cartão", description = "Edita um cartão de crédito existente")
    public ResponseEntity<CartaoCredito> editar(
            @PathVariable Long id,
            @Valid @RequestBody CartaoCreditoRequest request) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado");
        }

        CartaoCredito cartao = cartaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cartão não encontrado"));

        if (!cartao.getEmpresa().getId().equals(tenantId)) {
            throw new BusinessException("Cartão não pertence à sua empresa");
        }

        cartao.setNome(request.nome());
        cartao.setDiaVencimento(request.diaVencimento());
        if (request.diaFechamento() != null) {
            cartao.setDiaFechamento(request.diaFechamento());
        }
        if (request.limite() != null) {
            cartao.setLimite(request.limite());
        }

        CartaoCredito salvo = cartaoRepository.save(cartao);
        log.info("Cartao editado: {} (vencimento dia {}, fechamento dia {}, limite: R$ {})",
                salvo.getNome(), salvo.getDiaVencimento(), salvo.getDiaFechamento(), salvo.getLimite());
        return ResponseEntity.ok(salvo);
    }

    @GetMapping("/{id}/limite-disponivel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Consultar limite disponível", description = "Retorna o limite disponível do cartão")
    public ResponseEntity<LimiteDisponivelDTO> getLimiteDisponivel(
            @PathVariable Long id) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado");
        }

        CartaoCredito cartao = cartaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cartão não encontrado"));

        if (!cartao.getEmpresa().getId().equals(tenantId)) {
            throw new BusinessException("Cartão não pertence à sua empresa");
        }

        BigDecimal limiteDisponivel = faturaService.calcularLimiteDisponivel(cartao);
        BigDecimal limiteTotal = cartao.getLimite();
        BigDecimal limiteUtilizado = BigDecimal.ZERO;

        if (limiteTotal != null && limiteDisponivel != null) {
            limiteUtilizado = limiteTotal.subtract(limiteDisponivel);
        }

        return ResponseEntity.ok(new LimiteDisponivelDTO(
                limiteTotal,
                limiteDisponivel,
                limiteUtilizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar cartão", description = "Desativa um cartão de crédito (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Tenant ID não encontrado");
        }

        CartaoCredito cartao = cartaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cartão não encontrado"));

        if (!cartao.getEmpresa().getId().equals(tenantId)) {
            throw new BusinessException("Cartão não pertence à sua empresa");
        }

        cartao.setAtivo(false);
        cartaoRepository.save(cartao);
        log.info("Cartao desativado: {}", cartao.getNome());
        return ResponseEntity.noContent().build();
    }

    public record CartaoCreditoRequest(
            String nome,
            Integer diaVencimento,
            Integer diaFechamento,
            BigDecimal limite) {
    }

    public record LimiteDisponivelDTO(
            BigDecimal limiteTotal,
            BigDecimal limiteDisponivel,
            BigDecimal limiteUtilizado) {
    }
}
