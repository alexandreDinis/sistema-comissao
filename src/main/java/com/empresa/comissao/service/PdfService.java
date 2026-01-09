package com.empresa.comissao.service;

import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class PdfService {

    public byte[] gerarRelatorioFinanceiroPdf(RelatorioFinanceiroDTO relatorio) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Relatório Financeiro Mensal", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Period
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Período: " + relatorio.getMes() + "/" + relatorio.getAno(), fontSub));
            document.add(Chunk.NEWLINE);

            // Summary Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableRow(table, "Total Faturamento", formatCurrency(relatorio.getFaturamentoTotal()));
            addTableRow(table, "Total Adiantamentos", formatCurrency(relatorio.getAdiantamentosTotal()));
            addTableRow(table, "Total Comissões", formatCurrency(relatorio.getComissaoAlocada()));
            addTableRow(table, "Saldo Remanescente", formatCurrency(relatorio.getSaldoRemanescenteComissao()));

            document.add(table);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Detalhamento de Despesas", fontTitle));
            document.add(Chunk.NEWLINE);

            PdfPTable tableDespesas = new PdfPTable(2);
            tableDespesas.setWidthPercentage(100);
            tableDespesas.addCell(createHeaderCell("Categoria"));
            tableDespesas.addCell(createHeaderCell("Valor"));

            if (relatorio.getDespesasPorCategoria() != null) {
                relatorio.getDespesasPorCategoria().forEach((cat, val) -> {
                    tableDespesas.addCell(new PdfPCell(new Phrase(cat.name())));
                    tableDespesas.addCell(new PdfPCell(new Phrase(formatCurrency(val))));
                });
            }

            document.add(tableDespesas);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }

        return out.toByteArray();
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        table.addCell(new PdfPCell(new Phrase(label)));
        table.addCell(new PdfPCell(new Phrase(value)));
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null)
            return "R$ 0,00";
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);
    }
}
