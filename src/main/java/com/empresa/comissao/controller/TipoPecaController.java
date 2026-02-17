package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.TipoPeca;
import com.empresa.comissao.dto.request.TipoPecaRequest;
import com.empresa.comissao.service.TipoPecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-peca")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Peças", description = "Gestão do Catálogo de Peças e Serviços")
public class TipoPecaController {

    private final TipoPecaService tipoPecaService;

    @PostMapping
    @Operation(summary = "Adicionar item ao catálogo")
    public ResponseEntity<TipoPeca> criar(
            @Valid @RequestBody TipoPecaRequest request) {
        return ResponseEntity.ok(tipoPecaService.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar catálogo")
    public ResponseEntity<List<TipoPeca>> listar() {
        return ResponseEntity.ok(tipoPecaService.listarTodos());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir item do catálogo")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        tipoPecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
