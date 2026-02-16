package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.OrdemServico;
import com.empresa.comissao.dto.request.OrdemServicoRequest;
import com.empresa.comissao.dto.request.PecaServicoRequest;
import com.empresa.comissao.dto.request.VeiculoRequest;
import com.empresa.comissao.dto.response.OrdemServicoResponse;
import com.empresa.comissao.service.OrdemServicoService;
import com.empresa.comissao.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordem de Serviço", description = "Gestão completa de OS")
public class OrdemServicoController {

    private final OrdemServicoService osService;
    private final PdfService pdfService;

    @PostMapping
    @Operation(summary = "Criar nova OS")
    public ResponseEntity<OrdemServicoResponse> criarOS(@Valid @RequestBody OrdemServicoRequest request) {
        return ResponseEntity.ok(osService.criarOS(request));
    }

    @PostMapping("/veiculos")
    @Operation(summary = "Adicionar veículo à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarVeiculo(@Valid @RequestBody VeiculoRequest request) {
        return ResponseEntity.ok(osService.adicionarVeiculo(request));
    }

    @PostMapping("/pecas")
    @Operation(summary = "Adicionar peça/serviço ao veículo")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(@Valid @RequestBody PecaServicoRequest request) {
        return ResponseEntity.ok(osService.adicionarPeca(request));
    }

    @DeleteMapping("/pecas/{id}")
    @Operation(summary = "Remover peça/serviço do veículo")
    public ResponseEntity<OrdemServicoResponse> removerPeca(@PathVariable Long id) {
        return ResponseEntity.ok(osService.removerPeca(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar OS por ID")
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(osService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas as OS", description = "Suporte a Delta Sync (?since=ISO8601)")
    public ResponseEntity<java.util.List<OrdemServicoResponse>> listarTodas(
            @RequestParam(required = false) java.time.Instant since) {

        java.util.List<OrdemServicoResponse> result;
        long start = System.currentTimeMillis();

        if (since != null) {
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrdemServicoController.class);
            log.info("[OS] since(raw)={}", since);

            // Standardized conversion (UTC -> Local + Skew)
            java.time.LocalDateTime sinceLocal = com.empresa.comissao.util.SyncUtils.normalizeSince(since);

            log.info("[OS] sinceLocal(afterSkew)={}", sinceLocal);

            result = osService.listarSync(sinceLocal);
        } else {
            result = osService.listarSync(null);
        }

        long duration = System.currentTimeMillis() - start;
        org.slf4j.LoggerFactory.getLogger(OrdemServicoController.class)
                .info("[SYNC_METRIC] resource={}, items={}, duration={}ms", "ordens-servico", result.size(), duration);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/grid")
    @Operation(summary = "Listar OS com paginação e filtros (Web)", description = "Endpoint otimizado para grid do frontend")
    public ResponseEntity<org.springframework.data.domain.Page<OrdemServicoResponse>> listarGrid(
            org.springframework.data.domain.Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(required = false) Boolean atrasado) {

        // Ensure default sort if not present
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("data").descending());
        }

        return ResponseEntity.ok(osService.listarPaginated(pageable, status, search, date, atrasado));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar Status da OS")
    public ResponseEntity<OrdemServicoResponse> atualizarStatus(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String novoStatusStr = body.get("status");
        com.empresa.comissao.domain.enums.StatusOrdemServico novoStatus = com.empresa.comissao.domain.enums.StatusOrdemServico
                .valueOf(novoStatusStr);
        return ResponseEntity.ok(osService.atualizarStatus(id, novoStatus));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar OS (apenas campos selecionados, como desconto)")
    public ResponseEntity<OrdemServicoResponse> atualizar(@PathVariable Long id,
            @RequestBody @Valid com.empresa.comissao.dto.request.OrdemServicoPatchRequest request) {
        return ResponseEntity.ok(osService.atualizarOS(id, request));
    }

    @PatchMapping("/veiculos/{id}")
    @Operation(summary = "Atualizar veículo")
    public ResponseEntity<OrdemServicoResponse> atualizarVeiculo(@PathVariable Long id,
            @RequestBody VeiculoRequest request) {
        return ResponseEntity.ok(osService.atualizarVeiculo(id, request));
    }

    @PatchMapping("/pecas/{id}")
    @Operation(summary = "Atualizar peça/serviço")
    public ResponseEntity<OrdemServicoResponse> atualizarPeca(@PathVariable Long id,
            @RequestBody PecaServicoRequest request) {
        return ResponseEntity.ok(osService.atualizarPeca(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar OS")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        osService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download PDF da OS")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        OrdemServico os = osService.buscarEntidadePorId(id);
        byte[] pdfBytes = pdfService.gerarOrdemServicoPdf(os);

        // Montar nome do arquivo: empresa-os-123.pdf
        String nomeEmpresa = "empresa";
        if (os.getEmpresa() != null && os.getEmpresa().getNome() != null) {
            nomeEmpresa = os.getEmpresa().getNome()
                    .trim()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "-")
                    .replaceAll("-+", "-");
        }
        // Remove trailing hyphen if exists
        if (nomeEmpresa.endsWith("-")) {
            nomeEmpresa = nomeEmpresa.substring(0, nomeEmpresa.length() - 1);
        }

        String nomeArquivo = nomeEmpresa + "-os-" + id + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", nomeArquivo);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
