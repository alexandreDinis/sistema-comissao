package com.empresa.comissao.service;

import com.empresa.comissao.dto.response.VeiculoHistoricoResponse;
import com.empresa.comissao.domain.entity.VeiculoServico;
import com.empresa.comissao.repository.VeiculoServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoServicoRepository veiculoRepository;

    public Map<String, Object> verificarPlaca(String placa) {
        boolean exists = veiculoRepository.existsByPlaca(placa);
        return Map.of(
                "exists", exists,
                "message", exists ? "Veículo já possui histórico de serviços." : "Veículo novo.");
    }

    public List<VeiculoHistoricoResponse> obterHistorico(String placa) {
        List<VeiculoServico> veiculos = veiculoRepository.findByPlacaOrderByOrdemServicoDataDesc(placa);

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
