package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.dto.DespesaRequestDTO;
import com.empresa.comissao.service.ComissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/despesas")
@RequiredArgsConstructor
@Tag(name = "Despesas", description = "Endpoints para gerenciamento de gastos (alimentação, combustível, etc.)")
public class DespesaController {

    private final ComissaoService comissaoService;
    private final com.empresa.comissao.service.FinanceiroService financeiroService;
    private final com.empresa.comissao.service.FaturaService faturaService;
    private final com.empresa.comissao.repository.CartaoCreditoRepository cartaoRepository;
    private final com.empresa.comissao.repository.DespesaRepository despesaRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar uma nova despesa", description = "Adiciona um gasto categorizado ao sistema. Se cartão informado, agrupa em fatura.")
    public ResponseEntity<Despesa> criar(
            @Valid @RequestBody DespesaRequestDTO request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        // ... (mantém igual) ok, replace content abaixo vai substituir tudo

        // 1. Registrar a Despesa
        Despesa salva = comissaoService.adicionarDespesa(
                request.getDataDespesa(),
                request.getValor(),
                request.getCategoria(),
                request.getDescricao(),
                usuario);

        // 2. Se tem cartão, usar fluxo de fatura (agrupamento)
        if (request.getCartaoId() != null) {
            com.empresa.comissao.domain.entity.CartaoCredito cartao = cartaoRepository.findById(request.getCartaoId())
                    .orElseThrow(() -> new com.empresa.comissao.exception.BusinessException("Cartão não encontrado"));

            // Validar limite disponível antes de adicionar despesa
            faturaService.validarLimiteDisponivel(cartao, request.getValor());

            // Atualizar despesa com referência ao cartão
            salva.setCartao(cartao);
            salva = comissaoService.atualizarDespesa(salva);

            // Buscar ou criar fatura e atualizar valor
            com.empresa.comissao.domain.entity.ContaPagar fatura = faturaService.buscarOuCriarFatura(cartao,
                    request.getDataDespesa());
            faturaService.atualizarValorFatura(fatura);
        } else {
            // 3. Fluxo normal: criar ContaPagar individual
            if (salva != null) {
                financeiroService.criarContaPagarDeDespesa(
                        salva,
                        request.isPagoAgora(),
                        request.getDataVencimento(),
                        request.getMeioPagamento());
            }
        }

        return ResponseEntity.ok(salva);
    }

    @GetMapping
    @Operation(summary = "Listar despesas", description = "Retorna todas as despesas cadastradas")
    public ResponseEntity<List<Despesa>> listar(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        return ResponseEntity.ok(comissaoService.listarDespesas(usuario != null ? usuario.getEmpresa() : null));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir despesa", description = "Remove uma despesa e atualiza a fatura do cartão se necessário.")
    public ResponseEntity<Void> excluir(@org.springframework.web.bind.annotation.PathVariable Long id) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new com.empresa.comissao.exception.BusinessException("Despesa não encontrada"));

        com.empresa.comissao.domain.entity.CartaoCredito cartao = despesa.getCartao();
        java.time.LocalDate dataDespesa = despesa.getDataDespesa();

        despesaRepository.delete(despesa);

        if (cartao != null) {
            // Recalcular fatura associada
            try {
                com.empresa.comissao.domain.entity.ContaPagar fatura = faturaService.buscarOuCriarFatura(cartao,
                        dataDespesa);
                faturaService.atualizarValorFatura(fatura);
            } catch (Exception e) {
                // Fatura pode ter sido excluida ou nao existir mais, ignorar
            }
        }

        return ResponseEntity.noContent().build();
    }
}
