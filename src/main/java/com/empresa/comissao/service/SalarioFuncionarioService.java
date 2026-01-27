package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.SalarioFuncionario;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.TipoRemuneracao;
import com.empresa.comissao.dto.request.SalarioFuncionarioRequest;
import com.empresa.comissao.dto.response.SalarioFuncionarioResponse;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.SalarioFuncionarioRepository;
import com.empresa.comissao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciamento de salários e tipos de remuneração de
 * funcionários.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalarioFuncionarioService {

    private final SalarioFuncionarioRepository salarioFuncionarioRepository;
    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;

    /**
     * Lista todas as configurações de salário de uma empresa.
     */
    public List<SalarioFuncionarioResponse> listarPorEmpresa(Long empresaId) {
        log.info("📋 Listando configurações de salário para empresa ID: {}", empresaId);
        return salarioFuncionarioRepository.findByEmpresaIdOrderByDataInicioDesc(empresaId)
                .stream()
                .map(SalarioFuncionarioResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Busca a configuração de salário ativa de um funcionário.
     */
    public Optional<SalarioFuncionarioResponse> buscarAtivaPorFuncionario(Long usuarioId) {
        log.info("🔍 Buscando configuração de salário ativa para usuário ID: {}", usuarioId);
        return salarioFuncionarioRepository.findByUsuarioIdAndAtivoTrue(usuarioId)
                .map(SalarioFuncionarioResponse::fromEntity);
    }

    /**
     * Busca uma configuração por ID.
     */
    public SalarioFuncionarioResponse buscarPorId(Long id) {
        log.info("🔍 Buscando configuração de salário ID: {}", id);
        SalarioFuncionario salario = salarioFuncionarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Configuração de salário não encontrada: " + id));
        return SalarioFuncionarioResponse.fromEntity(salario);
    }

    /**
     * Cria ou atualiza a configuração de salário de um funcionário.
     * Desativa configurações anteriores.
     */
    @Transactional
    public SalarioFuncionarioResponse definir(Long empresaId, SalarioFuncionarioRequest request) {
        log.info("💰 Definindo salário para funcionário ID: {} na empresa ID: {}",
                request.getUsuarioId(), empresaId);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new BusinessException("Empresa não encontrada: " + empresaId));

        User usuario = userRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new BusinessException("Funcionário não encontrado: " + request.getUsuarioId()));

        // Verificar se o funcionário pertence à empresa
        if (usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("O funcionário não pertence a esta empresa");
        }

        validarRequest(request);

        // Desativar configuração anterior
        salarioFuncionarioRepository.findByUsuarioIdAndAtivoTrue(usuario.getId())
                .ifPresent(salarioAnterior -> {
                    salarioAnterior.setAtivo(false);
                    salarioAnterior.setDataFim(LocalDate.now().minusDays(1));
                    salarioFuncionarioRepository.save(salarioAnterior);
                    log.info("📴 Configuração anterior desativada: {}", salarioAnterior.getId());
                });

        // Criar nova configuração
        SalarioFuncionario salario = SalarioFuncionario.builder()
                .empresa(empresa)
                .usuario(usuario)
                .tipoRemuneracao(request.getTipoRemuneracao())
                .salarioBase(request.getSalarioBase())
                .percentualComissao(request.getPercentualComissao())
                .ativo(true)
                .dataInicio(request.getDataInicio() != null ? request.getDataInicio() : LocalDate.now())
                .dataFim(request.getDataFim())
                .build();

        salario = salarioFuncionarioRepository.save(salario);
        log.info("✅ Configuração de salário criada com ID: {}", salario.getId());

        return SalarioFuncionarioResponse.fromEntity(salario);
    }

    /**
     * Atualiza uma configuração de salário existente.
     */
    @Transactional
    public SalarioFuncionarioResponse atualizar(Long id, SalarioFuncionarioRequest request) {
        log.info("📝 Atualizando configuração de salário ID: {}", id);

        SalarioFuncionario salario = salarioFuncionarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Configuração de salário não encontrada: " + id));

        validarRequest(request);

        salario.setTipoRemuneracao(request.getTipoRemuneracao());
        salario.setSalarioBase(request.getSalarioBase());
        salario.setPercentualComissao(request.getPercentualComissao());
        salario.setDataInicio(request.getDataInicio());
        salario.setDataFim(request.getDataFim());

        salario = salarioFuncionarioRepository.save(salario);
        log.info("✅ Configuração atualizada: {}", salario.getId());

        return SalarioFuncionarioResponse.fromEntity(salario);
    }

    /**
     * Desativa uma configuração de salário.
     */
    @Transactional
    public void desativar(Long id) {
        log.info("📴 Desativando configuração de salário ID: {}", id);

        SalarioFuncionario salario = salarioFuncionarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Configuração de salário não encontrada: " + id));

        salario.setAtivo(false);
        salario.setDataFim(LocalDate.now());
        salarioFuncionarioRepository.save(salario);

        log.info("✅ Configuração desativada: {}", id);
    }

    /**
     * Busca a configuração ativa de um usuário para uma data específica.
     */
    public Optional<SalarioFuncionario> buscarConfigAtiva(User usuario, LocalDate data) {
        return salarioFuncionarioRepository.findActiveByUsuarioAndDate(usuario, data);
    }

    // ==================== Métodos Auxiliares ====================

    private void validarRequest(SalarioFuncionarioRequest request) {
        if (request.getTipoRemuneracao() == TipoRemuneracao.SALARIO_FIXO
                && request.getSalarioBase() == null) {
            throw new BusinessException("Salário base é obrigatório para tipo SALARIO_FIXO");
        }

        if (request.getTipoRemuneracao() == TipoRemuneracao.MISTA) {
            if (request.getSalarioBase() == null) {
                throw new BusinessException("Salário base é obrigatório para tipo MISTA");
            }
            if (request.getPercentualComissao() == null) {
                throw new BusinessException("Percentual de comissão é obrigatório para tipo MISTA");
            }
        }

        if (request.getDataFim() != null && request.getDataInicio() != null
                && request.getDataFim().isBefore(request.getDataInicio())) {
            throw new BusinessException("Data fim não pode ser anterior à data de início");
        }
    }
}
