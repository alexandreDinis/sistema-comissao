package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.FaixaComissaoConfig;
import com.empresa.comissao.domain.entity.RegraComissao;
import com.empresa.comissao.domain.enums.TipoRegraComissao;
import com.empresa.comissao.dto.request.RegraComissaoRequest;
import com.empresa.comissao.dto.response.RegraComissaoResponse;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FaixaComissaoConfigRepository;
import com.empresa.comissao.repository.RegraComissaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciamento de regras de comissionamento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegraComissaoService {

    private final RegraComissaoRepository regraComissaoRepository;
    private final FaixaComissaoConfigRepository faixaComissaoConfigRepository;
    private final EmpresaRepository empresaRepository;

    /**
     * Lista todas as regras de uma empresa.
     */
    public List<RegraComissaoResponse> listarPorEmpresa(Long empresaId) {
        log.info("📋 Listando regras de comissão para empresa ID: {}", empresaId);
        return regraComissaoRepository.findByEmpresaIdOrderByDataInicioDesc(empresaId)
                .stream()
                .map(RegraComissaoResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca uma regra por ID.
     */
    public RegraComissaoResponse buscarPorId(Long id) {
        log.info("🔍 Buscando regra de comissão ID: {}", id);
        RegraComissao regra = regraComissaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Regra de comissão não encontrada: " + id));
        return RegraComissaoResponse.fromEntity(regra);
    }

    /**
     * Busca a regra ativa de uma empresa.
     */
    public RegraComissaoResponse buscarRegraAtiva(Long empresaId) {
        log.info("🔍 Buscando regra ativa para empresa ID: {}", empresaId);
        RegraComissao regra = regraComissaoRepository.findActiveWithFaixasByEmpresa(
                empresaRepository.getReferenceById(empresaId))
                .orElseThrow(() -> new BusinessException("Nenhuma regra de comissão ativa para esta empresa"));
        return RegraComissaoResponse.fromEntity(regra);
    }

    /**
     * Cria uma nova regra de comissão.
     */
    @Transactional
    public RegraComissaoResponse criar(Long empresaId, RegraComissaoRequest request) {
        log.info("➕ Criando nova regra de comissão para empresa ID: {}", empresaId);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa não encontrada: " + empresaId));

        validarRequest(request);

        RegraComissao regra = RegraComissao.builder()
                .empresa(empresa)
                .nome(request.getNome())
                .tipoRegra(request.getTipoRegra())
                .ativo(false) // Novas regras começam inativas
                .descricao(request.getDescricao())
                .percentualFixo(request.getPercentualFixo())
                .dataInicio(request.getDataInicio())
                .dataFim(request.getDataFim())
                .build();

        regra = regraComissaoRepository.save(regra);

        // Criar faixas se o tipo exigir
        if (request.getFaixas() != null && !request.getFaixas().isEmpty()) {
            criarFaixas(regra, request.getFaixas());
        }

        log.info("✅ Regra criada com ID: {}", regra.getId());
        return RegraComissaoResponse.fromEntity(regra);
    }

    /**
     * Atualiza uma regra existente.
     */
    @Transactional
    public RegraComissaoResponse atualizar(Long id, RegraComissaoRequest request) {
        log.info("📝 Atualizando regra de comissão ID: {}", id);

        RegraComissao regra = regraComissaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Regra de comissão não encontrada: " + id));

        validarRequest(request);

        regra.setNome(request.getNome());
        regra.setTipoRegra(request.getTipoRegra());
        regra.setDescricao(request.getDescricao());
        regra.setPercentualFixo(request.getPercentualFixo());
        regra.setDataInicio(request.getDataInicio());
        regra.setDataFim(request.getDataFim());

        // Atualizar faixas
        if (request.getFaixas() != null) {
            faixaComissaoConfigRepository.deleteByRegraId(id);
            regra.getFaixas().clear();
            criarFaixas(regra, request.getFaixas());
        }

        regra = regraComissaoRepository.save(regra);
        log.info("✅ Regra atualizada: {}", regra.getId());
        return RegraComissaoResponse.fromEntity(regra);
    }

    /**
     * Ativa uma regra (desativando as outras da mesma empresa).
     */
    @Transactional
    public RegraComissaoResponse ativar(Long id) {
        log.info("✅ Ativando regra de comissão ID: {}", id);

        RegraComissao regra = regraComissaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Regra de comissão não encontrada: " + id));

        // Desativar regra atual
        regraComissaoRepository.findByEmpresaAndAtivoTrue(regra.getEmpresa())
                .ifPresent(regraAtual -> {
                    regraAtual.setAtivo(false);
                    regraComissaoRepository.save(regraAtual);
                    log.info("📴 Regra anterior desativada: {}", regraAtual.getId());
                });

        // Ativar nova regra
        regra.setAtivo(true);
        regra = regraComissaoRepository.save(regra);

        log.info("✅ Regra ativada com sucesso: {}", regra.getId());
        return RegraComissaoResponse.fromEntity(regra);
    }

    /**
     * Desativa uma regra.
     */
    @Transactional
    public void desativar(Long id) {
        log.info("📴 Desativando regra de comissão ID: {}", id);

        RegraComissao regra = regraComissaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Regra de comissão não encontrada: " + id));

        regra.setAtivo(false);
        regraComissaoRepository.save(regra);

        log.info("✅ Regra desativada: {}", regra.getId());
    }

    /**
     * Deleta uma regra (apenas se inativa).
     */
    @Transactional
    public void deletar(Long id) {
        log.info("🗑️ Deletando regra de comissão ID: {}", id);

        RegraComissao regra = regraComissaoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Regra de comissão não encontrada: " + id));

        if (regra.isAtivo()) {
            throw new BusinessException("Não é possível deletar uma regra ativa. Desative-a primeiro.");
        }

        regraComissaoRepository.delete(regra);
        log.info("✅ Regra deletada: {}", id);
    }

    // ==================== Métodos Auxiliares ====================

    private void validarRequest(RegraComissaoRequest request) {
        if (request.getTipoRegra() == TipoRegraComissao.FIXA_EMPRESA
                && request.getPercentualFixo() == null) {
            throw new BusinessException("Percentual fixo é obrigatório para regras do tipo FIXA_EMPRESA");
        }

        if ((request.getTipoRegra() == TipoRegraComissao.FAIXA_FATURAMENTO
                || request.getTipoRegra() == TipoRegraComissao.HIBRIDA)
                && (request.getFaixas() == null || request.getFaixas().isEmpty())) {
            throw new BusinessException("Faixas são obrigatórias para regras do tipo FAIXA_FATURAMENTO ou HIBRIDA");
        }

        if (request.getDataFim() != null && request.getDataFim().isBefore(request.getDataInicio())) {
            throw new BusinessException("Data fim não pode ser anterior à data de início");
        }
    }

    private void criarFaixas(RegraComissao regra, List<RegraComissaoRequest.FaixaComissaoRequest> faixasRequest) {
        int ordem = 1;
        for (RegraComissaoRequest.FaixaComissaoRequest faixaReq : faixasRequest) {
            FaixaComissaoConfig faixa = FaixaComissaoConfig.builder()
                    .regra(regra)
                    .minFaturamento(faixaReq.getMinFaturamento())
                    .maxFaturamento(faixaReq.getMaxFaturamento())
                    .porcentagem(faixaReq.getPorcentagem())
                    .descricao(faixaReq.getDescricao())
                    .ordem(faixaReq.getOrdem() != null ? faixaReq.getOrdem() : ordem++)
                    .build();
            regra.addFaixa(faixa);
        }
    }
}
