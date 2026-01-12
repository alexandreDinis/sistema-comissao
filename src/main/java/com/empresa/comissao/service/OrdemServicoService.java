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
        private final ComissaoService comissaoService;

        @Transactional
        public OrdemServicoResponse atualizarStatus(Long id,
                        com.empresa.comissao.domain.enums.StatusOrdemServico novoStatus) {
                log.info("🔄 Atualizando status da OS ID: {} para {}", id, novoStatus);

                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

                if (novoStatus == com.empresa.comissao.domain.enums.StatusOrdemServico.FINALIZADA
                                && os.getStatus() != com.empresa.comissao.domain.enums.StatusOrdemServico.FINALIZADA) {

                        log.info("💰 OS {} finalizada. Gerando faturamento...", id);

                        BigDecimal valorFaturamento = os.getValorTotal() != null ? os.getValorTotal() : BigDecimal.ZERO;

                        Faturamento faturamento = Faturamento.builder()
                                        .dataFaturamento(java.time.LocalDate.now())
                                        .valor(valorFaturamento)
                                        .ordemServico(os)
                                        .usuario(os.getUsuario())
                                        .empresa(os.getEmpresa())
                                        .build();

                        faturamentoRepository.save(faturamento);
                        log.info("✅ Faturamento gerado com sucesso para OS ID: {}", id);

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
        public void cancelar(Long id) {
                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

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

                OrdemServico os = OrdemServico.builder()
                                .cliente(cliente)
                                .data(request.getData())
                                .valorTotal(BigDecimal.ZERO)
                                .usuario(usuario)
                                .empresa(empresa)
                                .build();

                os = osRepository.save(os);
                return mapToResponse(os);
        }

        @Transactional
        public OrdemServicoResponse adicionarVeiculo(VeiculoRequest request) {
                OrdemServico os = osRepository.findById(request.getOrdemServicoId())
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));

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

                TipoPeca tipoPeca = tipoPecaRepository.findById(request.getTipoPecaId())
                                .orElseThrow(() -> new EntityNotFoundException("Peça não encontrada no catálogo"));

                BigDecimal valorFinal = request.getValorCobrado() != null ? request.getValorCobrado()
                                : tipoPeca.getValorPadrao();

                PecaServico peca = PecaServico.builder()
                                .veiculo(veiculo)
                                .tipoPeca(tipoPeca)
                                .valor(valorFinal)
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

        public OrdemServicoResponse buscarPorId(Long id) {
                OrdemServico os = osRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada"));
                return mapToResponse(os);
        }

        public java.util.List<OrdemServicoResponse> listarTodas() {
                return osRepository.findAll().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        private OrdemServicoResponse mapToResponse(OrdemServico os) {
                return OrdemServicoResponse.builder()
                                .id(os.getId())
                                .data(os.getData())
                                .status(os.getStatus())
                                .valorTotal(os.getValorTotal())
                                .cliente(ClienteResponse.builder()
                                                .id(os.getCliente().getId())
                                                .razaoSocial(os.getCliente().getRazaoSocial())
                                                .nomeFantasia(os.getCliente().getNomeFantasia())
                                                .cnpj(os.getCliente().getCnpj())
                                                .contato(os.getCliente().getContato())
                                                .build())
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
                                                .build()).collect(Collectors.toList()))
                                .build();
        }
}
