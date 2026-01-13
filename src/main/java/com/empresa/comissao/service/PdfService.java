package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.dto.RelatorioAnualDTO;
import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    private final TemplateEngine templateEngine;

    @Value("${app.upload.dir:uploads/logos}")
    private String uploadDir;

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
        Path path = Paths.get(uploadDir, logoPath);
        if (Files.exists(path)) {
            byte[] imageBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(imageBytes);
        }
        return null;
    }

    private byte[] htmlToPdf(String html) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
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
