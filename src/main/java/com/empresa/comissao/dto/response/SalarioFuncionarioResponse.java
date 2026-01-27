package com.empresa.comissao.dto.response;

import com.empresa.comissao.domain.entity.SalarioFuncionario;
import com.empresa.comissao.domain.enums.TipoRemuneracao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta para configuração de salário/remuneração de funcionário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarioFuncionarioResponse {

    private Long id;
    private Long empresaId;
    private String empresaNome;
    private Long usuarioId;
    private String usuarioEmail;
    private TipoRemuneracao tipoRemuneracao;
    private BigDecimal salarioBase;
    private BigDecimal percentualComissao;
    private boolean ativo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /**
     * Converte uma entidade para DTO.
     */
    public static SalarioFuncionarioResponse fromEntity(SalarioFuncionario entity) {
        return SalarioFuncionarioResponse.builder()
                .id(entity.getId())
                .empresaId(entity.getEmpresa() != null ? entity.getEmpresa().getId() : null)
                .empresaNome(entity.getEmpresa() != null ? entity.getEmpresa().getNome() : null)
                .usuarioId(entity.getUsuario() != null ? entity.getUsuario().getId() : null)
                .usuarioEmail(entity.getUsuario() != null ? entity.getUsuario().getEmail() : null)
                .tipoRemuneracao(entity.getTipoRemuneracao())
                .salarioBase(entity.getSalarioBase())
                .percentualComissao(entity.getPercentualComissao())
                .ativo(entity.isAtivo())
                .dataInicio(entity.getDataInicio())
                .dataFim(entity.getDataFim())
                .dataCriacao(entity.getDataCriacao())
                .dataAtualizacao(entity.getDataAtualizacao())
                .build();
    }
}
