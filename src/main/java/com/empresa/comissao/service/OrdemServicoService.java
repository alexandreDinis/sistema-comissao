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

import java.math.BigDecimal;
import java.time.LocalDate;
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
        }

        @Transactional
        public OrdemServicoResponse criarOS(OrdemServicoRequest request) {
                Cliente cliente = clienteRepository.findById(request.getClienteId())
                                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

                org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                com.empresa.comissao.domain.entity.User usuario = null;
                com.empresa.comissao.domain.entity.Empresa empresa = null;

                if (authentication != null
                                && authentication.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
                        usuario = (com.empresa.comissao.domain.entity.User) authentication.getPrincipal();
                        empresa = usuario.getEmpresa();
                }

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
                                .build();

                // Allow overriding user if provided (e.g. Admin creating for Salesperson)
                if (request.getUsuarioId() != null) {
                        com.empresa.comissao.domain.entity.User targetUser = userRepository
                                        .findById(request.getUsuarioId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Usuário indicado não encontrado"));
                        os.setUsuario(targetUser);
                }

                os = osRepository.save(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse adicionarVeiculo(VeiculoRequest request) {
                OrdemServico os = osRepository.findById(request.getOrdemServicoId())
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

                validarAcesso(os);

                // Validar e Normalizar placa
                String placaNormalizada = com.empresa.comissao.validation.ValidadorPlaca.normalizar(request.getPlaca());
                com.empresa.comissao.validation.ValidadorPlaca.validar(placaNormalizada);

                VeiculoServico veiculo = VeiculoServico.builder()
                                .ordemServico(os)
                                .placa(placaNormalizada)
                                .modelo(request.getModelo())
                                .cor(request.getCor())
                                .valorTotal(BigDecimal.ZERO)
                                .build();

                os.getVeiculos().add(veiculo);
                os = osRepository.save(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse adicionarPeca(PecaServicoRequest request) {
                VeiculoServico veiculo = veiculoRepository.findById(request.getVeiculoId())
                                .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado"));

                validarAcesso(veiculo.getOrdemServico());

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

                PecaServico peca = PecaServico.builder()
                                .veiculo(veiculo)
                                .tipoPeca(tipoPeca)
                                .valor(valorFinal)
                                .descricao(request.getDescricao())
                                .tipoExecucao(tipoExecucao)
                                .prestador(prestador)
                                .custoPrestador(request.getCustoPrestador())
                                .dataVencimentoPrestador(request.getDataVencimentoPrestador())
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

                log.info("🗑️ Peça ID {} removida da OS ID {}", pecaId, os.getId());

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
                // Get current user's empresa for tenant filtering
                org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                if (authentication != null
                                && authentication.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
                        com.empresa.comissao.domain.entity.User usuario = (com.empresa.comissao.domain.entity.User) authentication
                                        .getPrincipal();

                        if (usuario.getEmpresa() != null) {
                                return osRepository.findByEmpresa(usuario.getEmpresa()).stream()
                                                .map(this::mapToResponse)
                                                .collect(Collectors.toList());
                        }
                }

                // Fallback: return empty list if no empresa (security: don't expose all data)
                return java.util.Collections.emptyList();
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
                                .usuarioNome(os.getUsuario() != null ? os.getUsuario().getEmail() : null)
                                .usuarioEmail(os.getUsuario() != null ? os.getUsuario().getEmail() : null)
                                .veiculos(os.getVeiculos().stream().map(this::mapVeiculo).collect(Collectors.toList()))
                                .build();
        }

        private VeiculoResponse mapVeiculo(VeiculoServico v) {
                return VeiculoResponse.builder()
                                .id(v.getId())
                                .placa(v.getPlaca())
                                .modelo(v.getModelo())
                                .cor(v.getCor())
                                .valorTotal(v.getValorTotal())
                                .pecas(v.getPecas().stream().map(p -> PecaServicoResponse.builder()
                                                .id(p.getId())
                                                .nomePeca(p.getTipoPeca().getNome())
                                                .valorCobrado(p.getValor())
                                                .descricao(p.getDescricao())
                                                .build()).collect(Collectors.toList()))
                                .build();
        }

        private void validarAcesso(OrdemServico os) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
                        com.empresa.comissao.domain.entity.User user = (com.empresa.comissao.domain.entity.User) auth
                                        .getPrincipal(); // fixed indentation
                        if (user.getEmpresa() != null) {
                                if (os.getEmpresa() == null
                                                || !os.getEmpresa().getId().equals(user.getEmpresa().getId())) {
                                        throw new EntityNotFoundException("OS não encontrada");
                                }
                        }
                }
        }
}
