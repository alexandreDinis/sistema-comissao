package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.TipoPeca;
import com.empresa.comissao.dto.request.TipoPecaRequest;
import com.empresa.comissao.repository.TipoPecaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoPecaService {

    private final TipoPecaRepository tipoPecaRepository;

    @Transactional
    public TipoPeca criar(TipoPecaRequest request, com.empresa.comissao.domain.entity.User usuario) {
        TipoPeca tipoPeca = TipoPeca.builder()
                .nome(request.getNome())
                .valorPadrao(request.getValorPadrao())
                .empresa(usuario.getEmpresa())
                .build();
        return tipoPecaRepository.save(tipoPeca);
    }

    public List<TipoPeca> listarTodos(com.empresa.comissao.domain.entity.User usuario) {
        if (usuario != null && usuario.getEmpresa() != null) {
            return tipoPecaRepository.findByEmpresa(usuario.getEmpresa());
        }
        return java.util.Collections.emptyList();
    }

    @Transactional
    public void deletar(Long id) {
        if (!tipoPecaRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Tipo de peça não encontrado");
        }
        tipoPecaRepository.deleteById(id);
    }
}
