package com.empresa.comissao.dto.response;

import com.empresa.comissao.domain.enums.TipoDesconto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class OrdemServicoResponse {
    private Long id;
    private LocalDate data;
    private com.empresa.comissao.domain.enums.StatusOrdemServico status;
    private ClienteResponse cliente;
    private BigDecimal valorTotal; // Legacy field for backward compatibility
    private List<VeiculoResponse> veiculos;

    // Discount fields
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotalSemDesconto;
    private BigDecimal valorTotalComDesconto;

    // Payment due date
    private LocalDate dataVencimento;
    private boolean atrasado; // true if status=EM_EXECUCAO and due date is past

    // Responsible User (Salesperson)
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioEmail;

    // Sync Fields
    private String localId;
    private java.time.LocalDateTime deletedAt;
    private java.time.LocalDateTime updatedAt;
}
