package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Faturamento;
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
@RequestMapping("/api/v1/faturamento")
@RequiredArgsConstructor
public class FaturamentoController {

    private final ComissaoService comissaoService;

    @PostMapping
    public ResponseEntity<Faturamento> registrarFaturamento(@RequestBody FaturamentoRequest request) {
        Faturamento faturamento = comissaoService.adicionarFaturamento(request.getDataFaturamento(),
                request.getValor());
        return new ResponseEntity<>(faturamento, HttpStatus.CREATED);
    }

    @Getter
    @Setter
    public static class FaturamentoRequest {
        private LocalDate dataFaturamento;
        private BigDecimal valor;
    }
}
