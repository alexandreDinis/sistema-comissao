package com.empresa.comissao.dto.response;

import com.empresa.comissao.domain.entity.FaixaComissaoConfig;
import com.empresa.comissao.domain.entity.RegraComissao;
import com.empresa.comissao.domain.enums.TipoRegraComissao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de resposta para regras de comissão.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegraComissaoResponse {

    private Long id;
    private Long empresaId;
    private String empresaNome;
    private String nome;
    private TipoRegraComissao tipoRegra;
    private boolean ativa;
    private String descricao;
    private BigDecimal percentualFixo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private List<FaixaComissaoResponse> faixas;

    /**
     * Converte uma entidade para DTO.
     */
    public static RegraComissaoResponse fromEntity(RegraComissao entity) {
        return RegraComissaoResponse.builder()
                .id(entity.getId())
                .empresaId(entity.getEmpresa() != null ? entity.getEmpresa().getId() : null)
                .empresaNome(entity.getEmpresa() != null ? entity.getEmpresa().getNome() : null)
                .nome(entity.getNome())
                .tipoRegra(entity.getTipoRegra())
                .ativa(entity.isAtivo())
                .descricao(entity.getDescricao())
                .percentualFixo(entity.getPercentualFixo())
                .dataInicio(entity.getDataInicio())
                .dataFim(entity.getDataFim())
                .dataCriacao(entity.getDataCriacao())
                .dataAtualizacao(entity.getDataAtualizacao())
                .faixas(entity.getFaixas() != null
                        ? entity.getFaixas().stream()
                                .map(FaixaComissaoResponse::fromEntity)
                                .collect(Collectors.toList())
                        : null)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaixaComissaoResponse {
        private Long id;
        private BigDecimal minFaturamento;
        private BigDecimal maxFaturamento;
        private BigDecimal porcentagem;
        private String descricao;
        private int ordem;

        public static FaixaComissaoResponse fromEntity(FaixaComissaoConfig entity) {
            return FaixaComissaoResponse.builder()
                    .id(entity.getId())
                    .minFaturamento(entity.getMinFaturamento())
                    .maxFaturamento(entity.getMaxFaturamento())
                    .porcentagem(entity.getPorcentagem())
                    .descricao(entity.getDescricao())
                    .ordem(entity.getOrdem())
                    .build();
        }
    }
}
