package com.empresa.comissao.service;

import com.empresa.comissao.dto.response.VeiculoHistoricoResponse;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.VeiculoServico;
import com.empresa.comissao.repository.VeiculoServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class VeiculoService {

    private final VeiculoServicoRepository veiculoRepository;

    /**
     * Verifica se uma placa já existe para uma empresa específica (tenant
     * isolation).
     */
    public Map<String, Object> verificarPlaca(String placaRaw, Empresa empresa) {
        // Validar e Normalizar
        String placa = com.empresa.comissao.validation.ValidadorPlaca.normalizar(placaRaw);
        com.empresa.comissao.validation.ValidadorPlaca.validar(placa);

        log.info("🔍 Verificando placa: {} para empresa: {}", placa, empresa.getNome());
        List<VeiculoServico> veiculos = veiculoRepository.findByPlacaIgnoreCaseAndEmpresa(placa, empresa);
        boolean exists = !veiculos.isEmpty();
        log.info("✅ Placa existe na empresa: {}", exists);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("existe", exists);
        response.put("mensagem", exists ? "Veículo já cadastrado." : "Veículo disponível para cadastro.");

        if (exists) {
            // Return the most recent one or a summary
            VeiculoServico recent = veiculos.get(0);
            response.put("veiculoExistente", Map.of(
                    "modelo", recent.getModelo(),
                    "cor", recent.getCor(),
                    "cliente", recent.getOrdemServico().getCliente().getNomeFantasia()));
        }

        return response;
    }

    /**
     * Obtém o histórico de um veículo para uma empresa específica (tenant
     * isolation).
     */
    public List<VeiculoHistoricoResponse> obterHistorico(String placa, Empresa empresa) {
        List<VeiculoServico> veiculos = veiculoRepository.findByPlacaAndEmpresaOrderByOrdemServicoDataDesc(placa,
                empresa);

        return veiculos.stream().map(v -> {
            List<String> pecas = v.getPecas().stream()
                    .map(p -> p.getTipoPeca().getNome())
                    .collect(Collectors.toList());

            return VeiculoHistoricoResponse.builder()
                    .ordemServicoId(v.getOrdemServico().getId())
                    .data(v.getOrdemServico().getData())
                    .status(v.getOrdemServico().getStatus().toString())
                    .valorTotalServico(v.getValorTotal())
                    .pecasOuServicos(pecas)
                    .build();
        }).collect(Collectors.toList());
    }
}
