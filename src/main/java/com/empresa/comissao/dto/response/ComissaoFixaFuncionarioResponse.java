package com.empresa.comissao.dto.response;

import com.empresa.comissao.domain.entity.ComissaoFixaFuncionario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta para comissão fixa por funcionário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComissaoFixaFuncionarioResponse {

    private Long id;
    private Long empresaId;
    private String empresaNome;
    private Long usuarioId;
    private String usuarioEmail;
    private BigDecimal porcentagem;
    private boolean ativo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    /**
     * Converte uma entidade para DTO.
     */
    public static ComissaoFixaFuncionarioResponse fromEntity(ComissaoFixaFuncionario entity) {
        return ComissaoFixaFuncionarioResponse.builder()
                .id(entity.getId())
                .empresaId(entity.getEmpresa() != null ? entity.getEmpresa().getId() : null)
                .empresaNome(entity.getEmpresa() != null ? entity.getEmpresa().getNome() : null)
                .usuarioId(entity.getUsuario() != null ? entity.getUsuario().getId() : null)
                .usuarioEmail(entity.getUsuario() != null ? entity.getUsuario().getEmail() : null)
                .porcentagem(entity.getPorcentagem())
                .ativo(entity.isAtivo())
                .dataInicio(entity.getDataInicio())
                .dataFim(entity.getDataFim())
                .dataCriacao(entity.getDataCriacao())
                .dataAtualizacao(entity.getDataAtualizacao())
                .build();
    }
}
