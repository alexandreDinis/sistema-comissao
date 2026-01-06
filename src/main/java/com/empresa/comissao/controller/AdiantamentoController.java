package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import com.empresa.comissao.service.ComissaoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/adiantamento")
@RequiredArgsConstructor
public class AdiantamentoController {

    private final ComissaoService comissaoService;

    @PostMapping
    public ResponseEntity<PagamentoAdiantado> registrarAdiantamento(@RequestBody AdiantamentoRequest request) {
        PagamentoAdiantado adiantamento = comissaoService.adicionarAdiantamento(request.getDataPagamento(),
                request.getValor());
        return new ResponseEntity<>(adiantamento, HttpStatus.CREATED);
    }

    @Getter
    @Setter
    public static class AdiantamentoRequest {
        private LocalDate dataPagamento;
        private BigDecimal valor;
    }
}
