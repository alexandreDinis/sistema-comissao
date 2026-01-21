package com.empresa.comissao.dto.request;

import com.empresa.comissao.domain.enums.TipoExecucao;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PecaServicoRequest {
    @NotNull(message = "ID do Veículo é obrigatório")
    private Long veiculoId;

    @NotNull(message = "ID do Tipo de Peça é obrigatório")
    private Long tipoPecaId;

    // Opcional, se nulo usa o valorPadrao do TipoPeca
    private BigDecimal valorCobrado;

    // Observações sobre o serviço (opcional)
    private String descricao;

    // ========================================
    // SERVIÇO TERCEIRIZADO
    // ========================================

    // Tipo de execução: INTERNO (padrão) ou TERCEIRIZADO
    private TipoExecucao tipoExecucao;

    // ID do prestador (obrigatório se TERCEIRIZADO)
    private Long prestadorId;

    // Custo a pagar ao prestador (opcional, para controle financeiro)
    private BigDecimal custoPrestador;

    // Data de vencimento do pagamento ao prestador (opcional, padrão: 7 dias após a
    // OS)
    private LocalDate dataVencimentoPrestador;
}
