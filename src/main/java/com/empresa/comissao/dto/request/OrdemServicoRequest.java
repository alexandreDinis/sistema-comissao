package com.empresa.comissao.dto.request;

import com.empresa.comissao.domain.enums.TipoDesconto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrdemServicoRequest {
    @NotNull(message = "ID do Cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    // Offline Sync
    private String localId;

    // Optional discount fields
    private TipoDesconto tipoDesconto;

    @DecimalMin(value = "0.0", message = "Valor do desconto deve ser positivo")
    @Max(value = 100, message = "Desconto percentual não pode exceder 100%")
    private BigDecimal valorDesconto;

    // Payment due date (optional - defaults to creation date)
    private LocalDate dataVencimento;

    // Optional: Assign specific salesperson (Admin only)
    private Long usuarioId;
}
