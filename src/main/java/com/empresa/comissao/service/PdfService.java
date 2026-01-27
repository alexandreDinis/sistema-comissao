package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.OrdemServico;
import com.empresa.comissao.domain.enums.StatusOrdemServico;
import com.empresa.comissao.domain.enums.TipoDesconto;
import com.empresa.comissao.dto.RelatorioAnualDTO;
import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.empresa.comissao.dto.RelatorioReceitaCaixaDTO;
import com.empresa.comissao.dto.RelatorioFluxoCaixaDTO;
import com.empresa.comissao.dto.RelatorioContasPagarDTO;
import com.empresa.comissao.dto.RelatorioDistribuicaoLucrosDTO;
import com.lowagie.text.DocumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.empresa.comissao.domain.enums.CategoriaDespesa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class PdfService {

    private final TemplateEngine templateEngine;
    private final StorageService storageService;

    @Value("${app.upload.dir:uploads/logos}")
    private String uploadDir;

    @Autowired
    public PdfService(TemplateEngine templateEngine, @Autowired(required = false) StorageService storageService) {
        this.templateEngine = templateEngine;
        this.storageService = storageService;
    }

    public byte[] gerarRelatorioAnualPdf(RelatorioAnualDTO relatorio, Empresa empresa) {
        try {
            // Prepare template context
            Context context = new Context(Locale.of("pt", "BR"));
            context.setVariable("empresa", prepararDadosEmpresa(empresa));
            context.setVariable("relatorio", relatorio);
            context.setVariable("dataGeracao", LocalDateTime.now());

            // Process HTML template
            String htmlContent = templateEngine.process("pdf/relatorio-anual", context);
            if (htmlContent == null) {
                throw new RuntimeException("Erro ao processar template: resultado nulo");
            }

            // Convert to PDF
            return htmlToPdf(htmlContent);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF do relatório anual", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    public byte[] gerarFluxoCaixaPdf(java.time.YearMonth periodo, BigDecimal entradas,
            BigDecimal saidas, Empresa empresa) {
        try {
            Context context = new Context(Locale.of("pt", "BR"));
            context.setVariable("empresa", prepararDadosEmpresa(empresa));
            context.setVariable("periodo", periodo.getMonth().getDisplayName(
                    java.time.format.TextStyle.FULL, Locale.of("pt", "BR")) + " " + periodo.getYear());
            context.setVariable("entradas", entradas != null ? entradas : BigDecimal.ZERO);
            context.setVariable("saidas", saidas != null ? saidas : BigDecimal.ZERO);
            context.setVariable("saldo", (entradas != null ? entradas : BigDecimal.ZERO)
                    .subtract(saidas != null ? saidas : BigDecimal.ZERO));
            context.setVariable("dataGeracao", LocalDateTime.now());

            String htmlContent = templateEngine.process("pdf/fluxo-caixa", context);
            if (htmlContent == null) {
                throw new RuntimeException("Erro ao processar template: resultado nulo");
            }

            return htmlToPdf(htmlContent);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF do fluxo de caixa", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    public byte[] gerarRelatorioFinanceiroPdf(RelatorioFinanceiroDTO relatorio, Empresa empresa) {
        try {
            // Prepare template context
            Context context = new Context(Locale.of("pt", "BR"));
            context.setVariable("empresa", prepararDadosEmpresa(empresa));
            context.setVariable("relatorio", relatorio);
            context.setVariable("dataGeracao", LocalDateTime.now());

            // Process refined expense data with pre-calculated percentages
            context.setVariable("despesasDetalhadas", prepararDespesasComPercentual(relatorio));

            // Process HTML template
            String htmlContent = templateEngine.process("pdf/relatorio-financeiro", context);
            if (htmlContent == null) {
                throw new RuntimeException("Erro ao processar template: resultado nulo");
            }

            // Convert to PDF
            return htmlToPdf(htmlContent);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF do relatório financeiro", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    public byte[] gerarRelatorioReceitaCaixaPdf(RelatorioReceitaCaixaDTO relatorio, Empresa empresa) {
        return gerarPdfGeneric("pdf/receita-caixa", relatorio, empresa);
    }

    public byte[] gerarRelatorioFluxoCaixaDetalhadoPdf(RelatorioFluxoCaixaDTO relatorio, Empresa empresa) {
        return gerarPdfGeneric("pdf/fluxo-caixa-detalhado", relatorio, empresa);
    }

    public byte[] gerarRelatorioContasPagarPdf(RelatorioContasPagarDTO relatorio, Empresa empresa) {
        return gerarPdfGeneric("pdf/contas-pagar", relatorio, empresa);
    }

    public byte[] gerarRelatorioDistribuicaoLucrosPdf(RelatorioDistribuicaoLucrosDTO relatorio, Empresa empresa) {
        return gerarPdfGeneric("pdf/distribuicao-lucros", relatorio, empresa);
    }

    // Método genérico para simplificar
    private byte[] gerarPdfGeneric(String template, Object relatorioObj, Empresa empresa) {
        try {
            Context context = new Context(Locale.of("pt", "BR"));
            context.setVariable("empresa", prepararDadosEmpresa(empresa));
            context.setVariable("relatorio", relatorioObj);
            context.setVariable("dataGeracao", LocalDateTime.now());

            String htmlContent = templateEngine.process(template, context);
            if (htmlContent == null) {
                throw new RuntimeException("Erro ao processar template " + template);
            }
            return htmlToPdf(htmlContent);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF " + template, e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private Map<String, Object> prepararDadosEmpresa(Empresa empresa) {
        Map<String, Object> dados = new HashMap<>();

        if (empresa != null) {
            dados.put("razaoSocial", empresa.getRazaoSocial() != null ? empresa.getRazaoSocial()
                    : (empresa.getNome() != null ? empresa.getNome() : ""));
            dados.put("cnpj", empresa.getCnpj() != null ? formatarCNPJ(empresa.getCnpj()) : "");
            dados.put("endereco", empresa.getEndereco() != null ? empresa.getEndereco() : "");
            dados.put("telefone", empresa.getTelefone() != null ? empresa.getTelefone() : "");
            dados.put("email", empresa.getEmail() != null ? empresa.getEmail() : "");

            // Load logo
            try {
                if (empresa.getLogoPath() != null) {
                    String logoBase64 = carregarLogoBase64(empresa.getLogoPath());
                    if (logoBase64 != null) {
                        dados.put("logoUrl", "data:image/png;base64," + logoBase64);
                        dados.put("hasLogo", true);
                    } else {
                        dados.put("hasLogo", false);
                    }
                } else {
                    dados.put("hasLogo", false);
                }
            } catch (Exception e) {
                log.warn("Erro ao carregar logo da empresa {}", empresa.getId(), e);
                dados.put("hasLogo", false);
            }
        }

        return dados;
    }

    private String carregarLogoBase64(String logoPath) throws IOException {
        // Try S3 storage first if available
        if (storageService != null) {
            byte[] imageBytes = storageService.getFileBytes(logoPath);
            if (imageBytes != null) {
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        }

        // Fallback to local filesystem
        Path path = Paths.get(uploadDir, logoPath);
        if (Files.exists(path)) {
            byte[] imageBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(imageBytes);
        }
        return null;
    }

    /**
     * Gera PDF da Ordem de Serviço com dados da empresa, cliente e veículos.
     */
    public byte[] gerarOrdemServicoPdf(OrdemServico os) {
        try {
            Context context = new Context(Locale.of("pt", "BR"));
            context.setVariable("empresa", prepararDadosEmpresa(os.getEmpresa()));
            context.setVariable("os", os);
            context.setVariable("dataGeracao", LocalDateTime.now());

            // Status description
            context.setVariable("statusDescricao", getStatusDescricao(os.getStatus()));

            // Cliente CNPJ formatado
            Map<String, Object> clienteData = new HashMap<>();
            if (os.getCliente() != null && os.getCliente().getCnpj() != null) {
                clienteData.put("cnpjFormatado", formatarCNPJ(os.getCliente().getCnpj()));
            }
            context.setVariable("cliente", clienteData);

            // Calcular desconto se aplicável
            BigDecimal descontoCalculado = calcularDesconto(os);
            context.setVariable("descontoCalculado", descontoCalculado);

            String htmlContent = templateEngine.process("pdf/ordem-servico", context);
            if (htmlContent == null) {
                throw new RuntimeException("Erro ao processar template: resultado nulo");
            }

            return htmlToPdf(htmlContent);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF da Ordem de Serviço {}", os.getId(), e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private String getStatusDescricao(StatusOrdemServico status) {
        return switch (status) {
            case ABERTA -> "Aberta";
            case EM_EXECUCAO -> "Em Execução";
            case FINALIZADA -> "Finalizada";
            case CANCELADA -> "Cancelada";
        };
    }

    private BigDecimal calcularDesconto(OrdemServico os) {
        if (os.getTipoDesconto() == null || os.getValorDesconto() == null ||
                os.getValorDesconto().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (os.getTipoDesconto() == TipoDesconto.PERCENTUAL) {
            return os.getValorTotalSemDesconto()
                    .multiply(os.getValorDesconto())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            return os.getValorDesconto();
        }
    }

    private byte[] htmlToPdf(String html) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(java.util.Objects.requireNonNull(html, "HTML content cannot be null"));
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    private String formatarCNPJ(String cnpj) {
        if (cnpj == null)
            return "";
        String clean = cnpj.replaceAll("[^0-9]", "");
        if (clean.length() == 14) {
            return clean.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return cnpj;
    }

    private List<Map<String, Object>> prepararDespesasComPercentual(RelatorioFinanceiroDTO relatorio) {
        List<Map<String, Object>> lista = new ArrayList<>();
        BigDecimal total = relatorio.getDespesasTotal();

        if (relatorio.getDespesasPorCategoria() != null) {
            for (Map.Entry<CategoriaDespesa, BigDecimal> entry : relatorio.getDespesasPorCategoria().entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("categoria", entry.getKey().getDescricao());
                item.put("valor", entry.getValue());

                BigDecimal percentual = BigDecimal.ZERO;
                if (total != null && total.compareTo(BigDecimal.ZERO) > 0 && entry.getValue() != null) {
                    percentual = entry.getValue()
                            .multiply(new BigDecimal("100"))
                            .divide(total, 1, RoundingMode.HALF_UP);
                }
                item.put("percentual", percentual);

                lista.add(item);
            }
        }
        return lista;
    }
}
