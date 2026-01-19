package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.CartaoCredito;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.CartaoCreditoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cartoes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cartões de Crédito", description = "Gerenciamento de cartões corporativos")
public class CartaoController {

    private final CartaoCreditoRepository cartaoRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Listar cartões", description = "Lista todos os cartões de crédito da empresa")
    public ResponseEntity<List<CartaoCredito>> listar(@AuthenticationPrincipal User usuario) {
        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }
        return ResponseEntity.ok(cartaoRepository.findByEmpresaAndAtivoTrueOrderByNomeAsc(empresa));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar cartão", description = "Cadastra um novo cartão de crédito corporativo")
    public ResponseEntity<CartaoCredito> criar(
            @Valid @RequestBody CartaoCreditoRequest request,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        CartaoCredito cartao = CartaoCredito.builder()
                .nome(request.nome())
                .diaVencimento(request.diaVencimento())
                .empresa(empresa)
                .ativo(true)
                .build();

        CartaoCredito salvo = cartaoRepository.save(cartao);
        log.info("💳 Cartão criado: {} (vencimento dia {})", salvo.getNome(), salvo.getDiaVencimento());
        return ResponseEntity.ok(salvo);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar cartão", description = "Desativa um cartão de crédito (soft delete)")
    public ResponseEntity<Void> desativar(@PathVariable Long id, @AuthenticationPrincipal User usuario) {
        CartaoCredito cartao = cartaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cartão não encontrado"));

        if (!cartao.getEmpresa().getId().equals(usuario.getEmpresa().getId())) {
            throw new BusinessException("Cartão não pertence à sua empresa");
        }

        cartao.setAtivo(false);
        cartaoRepository.save(cartao);
        log.info("💳 Cartão desativado: {}", cartao.getNome());
        return ResponseEntity.noContent().build();
    }

    public record CartaoCreditoRequest(
            String nome,
            Integer diaVencimento) {
    }
}
