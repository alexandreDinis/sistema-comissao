package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.dto.request.OrdemServicoRequest;
import com.empresa.comissao.dto.request.PecaServicoRequest;
import com.empresa.comissao.dto.request.VeiculoRequest;
import com.empresa.comissao.dto.response.*;
import com.empresa.comissao.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class OrdemServicoService {

        private final OrdemServicoRepository osRepository;
        private final ClienteRepository clienteRepository;
        private final TipoPecaRepository tipoPecaRepository;
        private final VeiculoServicoRepository veiculoRepository;
        private final FaturamentoRepository faturamentoRepository;
        private final PecaServicoRepository pecaRepository;
        private final PrestadorRepository prestadorRepository;
        private final ComissaoService comissaoService;
        private final FinanceiroService financeiroService;
        private final UserRepository userRepository;
        private final ContaReceberRepository contaReceberRepository;

        @Autowired
        private TenantVersionService tenantVersionService;

        // ... existing fields ...

        private void bumpTenantVersion(OrdemServico os) {
                if (os != null && os.getEmpresa() != null) {
                        tenantVersionService.bump(os.getEmpresa().getId());
                }
        }

        /**
         * 🔐 Security: Valida se o usuário pode ser atribuído a uma OS deste tenant.
         * Deve pertencer à mesma empresa e ter role FUNCIONARIO ou ADMIN_EMPRESA.
         */
        private void validarAtribuicaoUsuario(User user, Empresa empresa) {
                if (user == null)
                        return;

                if (user.getEmpresa() == null || !user.getEmpresa().getId().equals(empresa.getId())) {
                        throw new IllegalArgumentException("O usuário selecionado não pertence a esta empresa.");
                }

                com.empresa.comissao.domain.enums.Role role = user.getRole();
                if (role != com.empresa.comissao.domain.enums.Role.ADMIN_EMPRESA
                                && role != com.empresa.comissao.domain.enums.Role.FUNCIONARIO) {
                        throw new IllegalArgumentException(
                                        "O usuário selecionado não tem permissão para ser um responsável técnico (Role inválida).");
                }
        }

        @Transactional
        public OrdemServicoResponse atualizarStatus(Long id,
                        com.empresa.comissao.domain.enums.StatusOrdemServico novoStatus) {
                log.info("🔄 Atualizando status da OS ID: {} para {}", id, novoStatus);

                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

                validarAcesso(os);

                // Validation: Cannot start OS without a responsible user
                if (novoStatus == com.empresa.comissao.domain.enums.StatusOrdemServico.EM_EXECUCAO) {
                        if (os.getUsuario() == null) {
                                throw new IllegalStateException(
                                                "Não é possível iniciar a OS sem um responsável técnico definido. Edite a OS e atribua um responsável.");
                        }
                }

                if (novoStatus == com.empresa.comissao.domain.enums.StatusOrdemServico.FINALIZADA
                                && os.getStatus() != com.empresa.comissao.domain.enums.StatusOrdemServico.FINALIZADA) {

                        log.info("💰 OS {} finalizada. Gerando faturamento...", id);

                        BigDecimal valorFaturamento = os.getValorTotal() != null ? os.getValorTotal() : BigDecimal.ZERO;

                        // Usar a data da OS como referência para evitar problemas de timezone
                        java.time.LocalDate dataReferencia = os.getData() != null
                                        ? os.getData()
                                        : java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));

                        Faturamento faturamento = Faturamento.builder()
                                        .dataFaturamento(dataReferencia)
                                        .valor(valorFaturamento)
                                        .ordemServico(os)
                                        .usuario(os.getUsuario())
                                        .empresa(os.getEmpresa())
                                        .build();

                        faturamentoRepository.save(faturamento);
                        log.info("✅ Faturamento gerado com sucesso para OS ID: {} (data: {})", id, dataReferencia);

                        bumpTenantVersion(os);

                        // Criar Conta a Receber automaticamente como PENDENTE
                        // Cliente precisa pagar para comissão ser computada
                        try {
                                // Data de vencimento: usar a da OS ou padrão 30 dias a partir da data da OS
                                java.time.LocalDate vencimento = os.getDataVencimento() != null
                                                ? os.getDataVencimento()
                                                : dataReferencia.plusDays(30);

                                financeiroService.criarContaReceberDeFaturamento(
                                                faturamento,
                                                vencimento,
                                                false, // PENDENTE - aguarda recebimento
                                                null); // Meio de pagamento definido ao receber
                                log.info("💰 ContaReceber PENDENTE criada para OS ID: {} (vencimento: {})", id,
                                                vencimento);
                        } catch (Exception e) {
                                log.warn("⚠️ Erro ao criar ContaReceber: {}", e.getMessage());
                        }

                        // ========================================
                        // CONTAS A PAGAR PARA PRESTADORES TERCEIRIZADOS
                        // ========================================
                        try {
                                // Reutiliza dataReferencia definida acima

                                for (VeiculoServico veiculo : os.getVeiculos()) {
                                        for (PecaServico peca : veiculo.getPecas()) {
                                                if (peca.isTerceirizado() && peca.getCustoPrestador() != null
                                                                && peca.getCustoPrestador()
                                                                                .compareTo(BigDecimal.ZERO) > 0) {

                                                        String descricao = String.format("OS #%d - %s - %s",
                                                                        os.getId(),
                                                                        peca.getTipoPeca().getNome(),
                                                                        peca.getPrestador().getNome());

                                                        // Usar data definida pelo usuário ou 7 dias como padrão
                                                        java.time.LocalDate vencimento = peca
                                                                        .getDataVencimentoPrestador() != null
                                                                                        ? peca.getDataVencimentoPrestador()
                                                                                        : dataReferencia.plusDays(7);

                                                        financeiroService.criarContaPagarPrestador(
                                                                        peca.getPrestador(),
                                                                        peca.getCustoPrestador(),
                                                                        descricao,
                                                                        vencimento,
                                                                        dataReferencia, // Passar data de competência
                                                                        os.getEmpresa());

                                                        log.info("💸 ContaPagar criada para prestador {} - R$ {} (competência: {}, vencimento: {})",
                                                                        peca.getPrestador().getNome(),
                                                                        peca.getCustoPrestador(),
                                                                        dataReferencia,
                                                                        vencimento);
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                log.warn("⚠️ Erro ao criar ContaPagar de prestador: {}", e.getMessage());
                        }

                        if (os.getUsuario() != null) {
                                comissaoService.invalidarCache(os.getUsuario(),
                                                java.time.YearMonth.from(faturamento.getDataFaturamento()));
                        }
                }

                os.setStatus(novoStatus);
                os = osRepository.save(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse atualizarOS(Long id,
                        com.empresa.comissao.dto.request.OrdemServicoPatchRequest request) {
                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

                validarAcesso(os);

                // Validation similar to create
                if (request.getTipoDesconto() != null && request.getValorDesconto() != null) {
                        if (request.getTipoDesconto() == com.empresa.comissao.domain.enums.TipoDesconto.PERCENTUAL &&
                                        request.getValorDesconto().compareTo(new BigDecimal("100")) > 0) {
                                throw new IllegalArgumentException("Desconto percentual não pode exceder 100%");
                        }
                }

                if (request.getData() != null) {
                        os.setData(request.getData());
                }

                boolean recalcular = false;
                if (request.getTipoDesconto() != null) {
                        os.setTipoDesconto(request.getTipoDesconto());
                        recalcular = true;
                }
                if (request.getValorDesconto() != null) {
                        os.setValorDesconto(request.getValorDesconto());
                        recalcular = true;
                }

                // If user sets value to 0, it might be sent as 0, but if null it means no
                // change?
                // Wait, if I want to remove discount? DTO sends null.
                // But my DTO logic above means "if not null, update".
                // How to clear discount?
                // Maybe if tipoDesconto is sent?
                // For now assuming we are applying/updating discount.

                if (recalcular) {
                        os.recalcularTotal();
                }

                // Handle Salesperson Assignment (Admin/Manager only)
                // Handle Salesperson Assignment (Admin/Manager only)
                if (request.getUsuarioId() != null) {
                        Long currentUserId = os.getUsuario() != null ? os.getUsuario().getId() : null;

                        if (currentUserId == null || !currentUserId.equals(request.getUsuarioId())) {
                                log.info("bust: Updating OS User from {} to {}", currentUserId,
                                                request.getUsuarioId());
                                com.empresa.comissao.domain.entity.User newUser = userRepository
                                                .findById(request.getUsuarioId())
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Usuário não encontrado"));

                                validarAtribuicaoUsuario(newUser, os.getEmpresa());

                                os.setUsuario(newUser);

                                // Propagate to Faturamento and ContaReceber if they exist
                                faturamentoRepository.findByOrdemServico(os).ifPresent(faturamento -> {
                                        faturamento.setUsuario(newUser);
                                        faturamentoRepository.save(faturamento);

                                        log.info("bust: Updated Faturamento owner to {}", newUser.getEmail());

                                        // Update ContaReceber
                                        contaReceberRepository.findByFaturamentoId(faturamento.getId())
                                                        .ifPresent(conta -> {
                                                                conta.setFuncionarioResponsavel(newUser);
                                                                contaReceberRepository.save(conta);
                                                                log.info("bust: Updated ContaReceber owner to {}",
                                                                                newUser.getEmail());
                                                        });
                                });
                        }
                }

                os = osRepository.save(os);
                bumpTenantVersion(os);
                return mapToResponse(os);
        }

        @Transactional
        public void cancelar(Long id) {
                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

                validarAcesso(os);

                if (os.getStatus() == com.empresa.comissao.domain.enums.StatusOrdemServico.FINALIZADA) {
                        throw new IllegalStateException("Não é possível cancelar uma OS finalizada");
                }

                os.setStatus(com.empresa.comissao.domain.enums.StatusOrdemServico.CANCELADA);
                osRepository.save(os);
                bumpTenantVersion(os);
        }

        @Transactional
        public OrdemServicoResponse criarOS(OrdemServicoRequest request) {
                com.empresa.comissao.domain.entity.Empresa empresa = getEmpresaAutenticada();

                // 🔄 Upsert / Idempotency based on localId
                if (request.getLocalId() != null) {
                        java.util.Optional<OrdemServico> existing = osRepository.findByLocalIdAndEmpresa(
                                        request.getLocalId(),
                                        empresa);
                        if (existing.isPresent()) {
                                log.info("🔄 Upsert OS (LocalID: {}): Atualizando existente ID {}",
                                                request.getLocalId(),
                                                existing.get().getId());
                                return atualizarOSExistente(existing.get(), request);
                        }
                }

                Cliente cliente = clienteRepository.findById(request.getClienteId())
                                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

                com.empresa.comissao.domain.entity.User usuario = getUserAutenticado();

                // Validate discount
                if (request.getTipoDesconto() != null && request.getValorDesconto() != null) {
                        if (request.getTipoDesconto() == com.empresa.comissao.domain.enums.TipoDesconto.PERCENTUAL &&
                                        request.getValorDesconto().compareTo(new BigDecimal("100")) > 0) {
                                throw new IllegalArgumentException("Desconto percentual não pode exceder 100%");
                        }
                }

                OrdemServico os = OrdemServico.builder()
                                .cliente(cliente)
                                .data(request.getData())
                                .dataVencimento(request.getDataVencimento() != null ? request.getDataVencimento()
                                                : request.getData())
                                .valorTotal(BigDecimal.ZERO)
                                .empresa(empresa)
                                .localId(request.getLocalId()) // Set localId from request
                                .build();

                // Allow overriding user if provided (e.g. Admin creating for Salesperson)
                if (request.getUsuarioId() != null) {
                        com.empresa.comissao.domain.entity.User targetUser = userRepository
                                        .findById(request.getUsuarioId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Usuário indicado não encontrado"));

                        validarAtribuicaoUsuario(targetUser, empresa);

                        os.setUsuario(targetUser);
                } else if (usuario != null) {
                        os.setUsuario(usuario);
                }

                os = osRepository.save(os);
                bumpTenantVersion(os);
                return mapToResponse(os);
        }

        private OrdemServicoResponse atualizarOSExistente(OrdemServico os, OrdemServicoRequest request) {
                // Update basic fields
                os.setData(request.getData());
                if (request.getDataVencimento() != null) {
                        os.setDataVencimento(request.getDataVencimento());
                }

                // Update Discount
                boolean recalcular = false;
                if (request.getTipoDesconto() != null) {
                        os.setTipoDesconto(request.getTipoDesconto());
                        recalcular = true;
                }
                if (request.getValorDesconto() != null) {
                        os.setValorDesconto(request.getValorDesconto());
                        recalcular = true;
                }

                if (recalcular) {
                        os.recalcularTotal();
                }

                // Update User if needed
                if (request.getUsuarioId() != null) {
                        com.empresa.comissao.domain.entity.User targetUser = userRepository
                                        .findById(request.getUsuarioId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Usuário indicado não encontrado"));

                        validarAtribuicaoUsuario(targetUser, os.getEmpresa());

                        os.setUsuario(targetUser);
                }

                os = osRepository.save(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse adicionarVeiculo(VeiculoRequest request) {
                com.empresa.comissao.domain.entity.Empresa empresa = getEmpresaAutenticada();
                OrdemServico os;

                // 1. Resolve OS (ID or LocalID)
                if (request.getOrdemServicoId() != null) {
                        os = osRepository.findById(request.getOrdemServicoId())
                                        .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
                        if (os.getEmpresa() == null || !os.getEmpresa().getId().equals(empresa.getId())) {
                                throw new EntityNotFoundException("OS não encontrada");
                        }
                } else if (request.getOrdemServicoLocalId() != null) {
                        os = osRepository.findByLocalIdAndEmpresa(request.getOrdemServicoLocalId(), empresa)
                                        .orElseThrow(() -> new com.empresa.comissao.exception.DependencyNotFoundException(
                                                        "OS não encontrada para localId="
                                                                        + request.getOrdemServicoLocalId(),
                                                        "OS"));
                } else {
                        throw new IllegalArgumentException("ID ou LocalID da OS é obrigatório");
                }

                // Validar e Normalizar placa
                String placaNormalizada = com.empresa.comissao.validation.ValidadorPlaca.normalizar(request.getPlaca());
                com.empresa.comissao.validation.ValidadorPlaca.validar(placaNormalizada);

                // 2. Upsert Veiculo check (Explicit Scope: Company derived from OS)
                if (request.getLocalId() != null) {
                        java.util.Optional<VeiculoServico> existing = veiculoRepository
                                        .findByLocalIdAndOrdemServico_Empresa(request.getLocalId(), empresa);
                        if (existing.isPresent()) {
                                VeiculoServico v = existing.get();
                                log.info("🔄 Upsert Veículo (LocalID: {}): Atualizando existente ID {}",
                                                request.getLocalId(), v.getId());
                                v.setPlaca(placaNormalizada);
                                v.setModelo(request.getModelo());
                                v.setCor(request.getCor());

                                // Ensure strict parent consistency (optional but good for data integrity)
                                if (!v.getOrdemServico().getId().equals(os.getId())) {
                                        log.warn("⚠️ Veículo movido de OS ID {} para OS ID {}",
                                                        v.getOrdemServico().getId(), os.getId());
                                        // In a pure sync scenario, reparenting might happen?
                                        // For now, let's assume it stays in same OS or just update ref
                                        v.setOrdemServico(os);
                                }

                                veiculoRepository.save(v);
                                return mapToResponse(v.getOrdemServico());
                        }
                }

                VeiculoServico veiculo = VeiculoServico.builder()
                                .ordemServico(os)
                                .placa(placaNormalizada)
                                .modelo(request.getModelo())
                                .cor(request.getCor())
                                .valorTotal(BigDecimal.ZERO)
                                .localId(request.getLocalId()) // Set localId
                                .build();

                os.getVeiculos().add(veiculo);
                os = osRepository.save(os);
                bumpTenantVersion(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse adicionarPeca(PecaServicoRequest request) {
                com.empresa.comissao.domain.entity.Empresa empresa = getEmpresaAutenticada();
                VeiculoServico veiculo;

                // 1. Resolve Veiculo (ID or LocalID)
                if (request.getVeiculoId() != null) {
                        veiculo = veiculoRepository.findById(request.getVeiculoId())
                                        .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado"));
                        if (veiculo.getOrdemServico().getEmpresa() == null
                                        || !veiculo.getOrdemServico().getEmpresa().getId().equals(empresa.getId())) {
                                throw new EntityNotFoundException("Veículo não encontrado");
                        }
                } else if (request.getVeiculoLocalId() != null) {
                        veiculo = veiculoRepository
                                        .findByLocalIdAndOrdemServico_Empresa(request.getVeiculoLocalId(), empresa)
                                        .orElseThrow(() -> new com.empresa.comissao.exception.DependencyNotFoundException(
                                                        "Veículo não encontrado para localId="
                                                                        + request.getVeiculoLocalId(),
                                                        "VEICULO"));
                } else {
                        throw new IllegalArgumentException("ID ou LocalID do Veículo é obrigatório");
                }

                TipoPeca tipoPeca = tipoPecaRepository.findById(request.getTipoPecaId())
                                .orElseThrow(() -> new EntityNotFoundException("Peça não encontrada no catálogo"));

                // Validate Tenant for TipoPeca
                if (tipoPeca.getEmpresa() != null && veiculo.getOrdemServico().getEmpresa() != null) {
                        if (!tipoPeca.getEmpresa().getId().equals(veiculo.getOrdemServico().getEmpresa().getId())) {
                                throw new EntityNotFoundException("Peça não encontrada no catálogo desta empresa");
                        }
                }

                BigDecimal valorFinal = request.getValorCobrado() != null ? request.getValorCobrado()
                                : tipoPeca.getValorPadrao();

                // Determinar tipo de execução (padrão: INTERNO)
                com.empresa.comissao.domain.enums.TipoExecucao tipoExecucao = request.getTipoExecucao() != null
                                ? request.getTipoExecucao()
                                : com.empresa.comissao.domain.enums.TipoExecucao.INTERNO;

                // Buscar prestador se TERCEIRIZADO
                Prestador prestador = null;
                if (tipoExecucao == com.empresa.comissao.domain.enums.TipoExecucao.TERCEIRIZADO) {
                        if (request.getPrestadorId() == null) {
                                throw new IllegalArgumentException(
                                                "Prestador é obrigatório para serviços terceirizados");
                        }
                        prestador = prestadorRepository.findById(request.getPrestadorId())
                                        .orElseThrow(() -> new EntityNotFoundException("Prestador não encontrado"));

                        // Validate Tenant for Prestador
                        if (prestador.getEmpresa() != null && veiculo.getOrdemServico().getEmpresa() != null) {
                                if (!prestador.getEmpresa().getId()
                                                .equals(veiculo.getOrdemServico().getEmpresa().getId())) {
                                        throw new EntityNotFoundException("Prestador não encontrado nesta empresa");
                                }
                        }

                        log.info("🔧 Serviço terceirizado: {} - Prestador: {}", tipoPeca.getNome(),
                                        prestador.getNome());
                }

                // 2. Upsert Peca check
                if (request.getLocalId() != null) {
                        java.util.Optional<PecaServico> existing = pecaRepository
                                        .findByLocalIdAndVeiculo_OrdemServico_Empresa(request.getLocalId(), empresa);
                        if (existing.isPresent()) {
                                PecaServico p = existing.get();
                                log.info("🔄 Upsert Peça (LocalID: {}): Atualizando existente ID {}",
                                                request.getLocalId(), p.getId());

                                // Update fields
                                p.setTipoPeca(tipoPeca);
                                p.setValor(valorFinal);
                                p.setDescricao(request.getDescricao());
                                p.setTipoExecucao(tipoExecucao);
                                p.setPrestador(prestador);
                                p.setCustoPrestador(request.getCustoPrestador());
                                p.setDataVencimentoPrestador(request.getDataVencimentoPrestador());

                                pecaRepository.save(p);

                                // Recalculate Totals
                                p.getVeiculo().recalcularTotal();
                                osRepository.save(p.getVeiculo().getOrdemServico());

                                return mapToResponse(p.getVeiculo().getOrdemServico());
                        }
                }

                PecaServico peca = PecaServico.builder()
                                .veiculo(veiculo)
                                .tipoPeca(tipoPeca)
                                .valor(valorFinal)
                                .descricao(request.getDescricao())
                                .tipoExecucao(tipoExecucao)
                                .prestador(prestador)
                                .custoPrestador(request.getCustoPrestador())
                                .dataVencimentoPrestador(request.getDataVencimentoPrestador())
                                .localId(request.getLocalId()) // Set localId
                                .build();

                // Add to vehicle list
                veiculo.getPecas().add(peca);

                // Recalculate Totals (Vehicle -> OS)
                veiculo.recalcularTotal(); // This updates Vehicle Total and calls OS.recalcularTotal()

                // Save
                veiculoRepository.save(veiculo); // Cascade saves parts
                // Saving vehicle should be enough if mapped correctly, but let's save OS to be
                // sure if relation is bidirectional managed
                // Veiculo has @ManyToOne to OS. OS has @OneToMany(cascade=ALL).
                // Saving Veiculo updates the child side.
                // To be safe and ensure OS total is persisted:
                osRepository.save(veiculo.getOrdemServico());

                bumpTenantVersion(veiculo.getOrdemServico());

                return mapToResponse(veiculo.getOrdemServico());
        }

        @Transactional
        public OrdemServicoResponse removerPeca(Long pecaId) {
                PecaServico peca = pecaRepository.findById(pecaId)
                                .orElseThrow(() -> new EntityNotFoundException("Peça/Serviço não encontrado"));

                VeiculoServico veiculo = peca.getVeiculo();
                OrdemServico os = veiculo.getOrdemServico();

                validarAcesso(os);

                // Verificar se a OS não está finalizada
                if (os.getStatus() == com.empresa.comissao.domain.enums.StatusOrdemServico.FINALIZADA) {
                        throw new IllegalStateException("Não é possível remover peças de uma OS finalizada");
                }

                // Remover a peça do veículo
                veiculo.getPecas().remove(peca);

                // Deletar a peça
                pecaRepository.delete(peca);

                // Recalcular totais
                veiculo.recalcularTotal();
                osRepository.save(os);
                bumpTenantVersion(os);

                log.info("🗑️ Peça ID {} removida da OS ID {}", pecaId, os.getId());

                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse atualizarVeiculo(Long id, VeiculoRequest request) {
                VeiculoServico veiculo = veiculoRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado"));

                OrdemServico os = veiculo.getOrdemServico();
                validarAcesso(os);

                // Normalizar e validar placa
                if (request.getPlaca() != null) {
                        String placaNormalizada = com.empresa.comissao.validation.ValidadorPlaca
                                        .normalizar(request.getPlaca());
                        com.empresa.comissao.validation.ValidadorPlaca.validar(placaNormalizada);
                        veiculo.setPlaca(placaNormalizada);
                }
                if (request.getModelo() != null) {
                        veiculo.setModelo(request.getModelo());
                }
                if (request.getCor() != null) {
                        veiculo.setCor(request.getCor());
                }

                veiculoRepository.save(veiculo);
                bumpTenantVersion(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse atualizarPeca(Long id, PecaServicoRequest request) {
                PecaServico peca = pecaRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Peça/Serviço não encontrado"));

                VeiculoServico veiculo = peca.getVeiculo();
                OrdemServico os = veiculo.getOrdemServico();
                validarAcesso(os);

                // Atualizar tipo de peça se informado
                if (request.getTipoPecaId() != null) {
                        TipoPeca tipoPeca = tipoPecaRepository.findById(request.getTipoPecaId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Peça não encontrada no catálogo"));
                        peca.setTipoPeca(tipoPeca);
                }

                // Atualizar valor
                if (request.getValorCobrado() != null) {
                        peca.setValor(request.getValorCobrado());
                }

                // Atualizar descrição
                if (request.getDescricao() != null) {
                        peca.setDescricao(request.getDescricao());
                }

                // Atualizar tipo de execução
                if (request.getTipoExecucao() != null) {
                        peca.setTipoExecucao(request.getTipoExecucao());
                }

                // Atualizar prestador
                if (request.getPrestadorId() != null) {
                        Prestador prestador = prestadorRepository.findById(request.getPrestadorId())
                                        .orElseThrow(() -> new EntityNotFoundException("Prestador não encontrado"));
                        peca.setPrestador(prestador);
                }
                if (request.getCustoPrestador() != null) {
                        peca.setCustoPrestador(request.getCustoPrestador());
                }
                if (request.getDataVencimentoPrestador() != null) {
                        peca.setDataVencimentoPrestador(request.getDataVencimentoPrestador());
                }

                pecaRepository.save(peca);

                // Recalcular totais
                veiculo.recalcularTotal();
                osRepository.save(os);
                bumpTenantVersion(os);

                return mapToResponse(os);
        }

        public OrdemServicoResponse buscarPorId(Long id) {
                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
                validarAcesso(os);
                return mapToResponse(os);
        }

        public OrdemServico buscarEntidadePorId(Long id) {
                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
                validarAcesso(os);
                return os;
        }

        public java.util.List<OrdemServicoResponse> listarTodas() {
                return listarSync(null);
        }

        public java.util.List<OrdemServicoResponse> listarSync(java.time.LocalDateTime since) {
                Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();

                if (tenantId != null) {
                        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                        .getContext().getAuthentication();

                        // Determine if user is Admin
                        boolean isAdmin = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA") ||
                                                        a.getAuthority().equals("ROLE_SUPER_ADMIN") ||
                                                        a.getAuthority().equals("ROLE_ADMIN_LICENCA"));

                        Long userId = null;
                        if (!isAdmin) {
                                if (auth.getPrincipal() instanceof com.empresa.comissao.security.AuthPrincipal) {
                                        userId = ((com.empresa.comissao.security.AuthPrincipal) auth.getPrincipal())
                                                        .getUserId();
                                } else if (auth.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
                                        userId = ((com.empresa.comissao.domain.entity.User) auth.getPrincipal())
                                                        .getId();
                                }
                        }

                        List<OrdemServico> list;
                        if (isAdmin) {
                                // ADMIN: vê tudo do tenant
                                if (since != null) {
                                        list = osRepository.findSyncData(tenantId, since);
                                } else {
                                        list = osRepository.findAllFullSync(tenantId);
                                }
                        } else {
                                // TECNICO/OUTROS: vê apenas suas OS
                                if (userId == null) {
                                        // Should not happen for authenticated non-admin users, but safe fallback
                                        return java.util.Collections.emptyList();
                                }
                                if (since != null) {
                                        list = osRepository.findSyncDataByUsuario(tenantId, since, userId);
                                } else {
                                        list = osRepository.findAllFullSyncByUsuario(tenantId, userId);
                                }
                        }

                        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
                }

                return java.util.Collections.emptyList();
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<OrdemServicoResponse> listarPaginated(
                        org.springframework.data.domain.Pageable pageable,
                        String status,
                        String search,
                        java.time.LocalDate date,
                        Boolean atrasado) {

                Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
                if (tenantId == null) {
                        return org.springframework.data.domain.Page.empty();
                }

                com.empresa.comissao.domain.entity.User usuario = null;
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                boolean isAdmin = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_EMPRESA") ||
                                                a.getAuthority().equals("ROLE_SUPER_ADMIN") ||
                                                a.getAuthority().equals("ROLE_ADMIN_LICENCA"));

                if (!isAdmin) {
                        // Simplify user retrieval logic
                        if (auth.getPrincipal() instanceof com.empresa.comissao.security.AuthPrincipal) {
                                Long userId = ((com.empresa.comissao.security.AuthPrincipal) auth.getPrincipal())
                                                .getUserId();
                                usuario = userRepository.findById(userId).orElse(null);
                        } else if (auth.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
                                usuario = (com.empresa.comissao.domain.entity.User) auth.getPrincipal();
                        }
                }

                // 1. First Query: Find IDs with Filters (Efficient Count + Pagination)
                org.springframework.data.jpa.domain.Specification<OrdemServico> spec = com.empresa.comissao.repository.spec.OrdemServicoSpecification
                                .withFilter(tenantId, usuario, status, search, date, atrasado);

                // Optimized Query: Find All with Filters AND Fetches (via Specification)
                // Note: The Specification now handles JOIN FETCH for relations to avoid N+1.
                org.springframework.data.domain.Page<OrdemServico> page = osRepository.findAll(spec, pageable);

                return page.map(this::mapToResponse);
        }

        private OrdemServicoResponse mapToResponse(OrdemServico os) {
                // Calculate if overdue: status=EM_EXECUCAO and due date past today
                boolean atrasado = os.getStatus() == com.empresa.comissao.domain.enums.StatusOrdemServico.EM_EXECUCAO
                                && os.getDataVencimento() != null
                                && os.getDataVencimento().isBefore(LocalDate.now());

                return OrdemServicoResponse.builder()
                                .id(os.getId())
                                .data(os.getData())
                                .dataVencimento(os.getDataVencimento())
                                .atrasado(atrasado)
                                .status(os.getStatus())
                                .valorTotal(os.getValorTotal())
                                .tipoDesconto(os.getTipoDesconto())
                                .valorDesconto(os.getValorDesconto())
                                .valorTotalSemDesconto(os.getValorTotalSemDesconto())
                                .valorTotalComDesconto(os.getValorTotalComDesconto())
                                .cliente(ClienteResponse.builder()
                                                .id(os.getCliente().getId())
                                                .razaoSocial(os.getCliente().getRazaoSocial())
                                                .nomeFantasia(os.getCliente().getNomeFantasia())
                                                .cnpj(os.getCliente().getCnpj())
                                                .contato(os.getCliente().getContato())
                                                .build())
                                .usuarioId(os.getUsuario() != null ? os.getUsuario().getId() : null)
                                .usuarioEmail(os.getUsuario() != null ? os.getUsuario().getEmail() : null)
                                .veiculos(os.getVeiculos().stream().map(this::mapVeiculo).collect(Collectors.toList()))
                                .localId(os.getLocalId())
                                .deletedAt(os.getDeletedAt())
                                .updatedAt(os.getUpdatedAt())
                                .build();
        }

        private VeiculoResponse mapVeiculo(VeiculoServico v) {
                return VeiculoResponse.builder()
                                .id(v.getId())
                                .localId(v.getLocalId())
                                .placa(v.getPlaca())
                                .modelo(v.getModelo())
                                .cor(v.getCor())
                                .valorTotal(v.getValorTotal())
                                .pecas(v.getPecas().stream().map(p -> PecaServicoResponse.builder()
                                                .id(p.getId())
                                                .localId(p.getLocalId())
                                                .tipoPecaId(p.getTipoPeca().getId())
                                                .nomePeca(p.getTipoPeca().getNome())
                                                .valorCobrado(p.getValor())
                                                .descricao(p.getDescricao())
                                                .build()).collect(Collectors.toList()))
                                .build();
        }

        private com.empresa.comissao.domain.entity.Empresa getEmpresaAutenticada() {
                Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
                if (tenantId != null) {
                        return com.empresa.comissao.domain.entity.Empresa.builder().id(tenantId).build();
                }
                throw new EntityNotFoundException("Usuário não vinculado a uma empresa");
        }

        private com.empresa.comissao.domain.entity.User getUserAutenticado() {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                if (auth != null && auth.getPrincipal() instanceof com.empresa.comissao.security.AuthPrincipal) {
                        com.empresa.comissao.security.AuthPrincipal principal = (com.empresa.comissao.security.AuthPrincipal) auth
                                        .getPrincipal();
                        return com.empresa.comissao.domain.entity.User.builder().id(principal.getUserId())
                                        .email(principal.getEmail()).build();
                }
                // Fallback for legacy tests or other auth methods
                if (auth != null && auth.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
                        return (com.empresa.comissao.domain.entity.User) auth.getPrincipal();
                }
                return null;
        }

        private void validarAcesso(OrdemServico os) {
                Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
                if (tenantId != null) {
                        if (os.getEmpresa() == null
                                        || !os.getEmpresa().getId().equals(tenantId)) {
                                throw new EntityNotFoundException("OS não encontrada");
                        }
                }
        }
}
