package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Cliente;
import com.empresa.comissao.dto.request.ClienteRequest;
import com.empresa.comissao.dto.response.ClienteResponse;
import com.empresa.comissao.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse criar(ClienteRequest request, com.empresa.comissao.domain.entity.User usuario) {
        com.empresa.comissao.validation.ValidadorDocumento.validarCnpj(request.getCnpj());

        Cliente cliente = new Cliente();
        updateEntity(cliente, request);
        cliente.setEmpresa(usuario.getEmpresa());
        cliente = clienteRepository.save(cliente);
        return mapToResponse(cliente);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));

        com.empresa.comissao.validation.ValidadorDocumento.validarCnpj(request.getCnpj());

        updateEntity(cliente, request);
        cliente = clienteRepository.save(cliente);
        return mapToResponse(cliente);
    }

    public void deletar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Cliente não encontrado");
        }
        clienteRepository.deleteById(id);
    }

    public ClienteResponse buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));
    }

    public List<ClienteResponse> listar(String termo, String cidade, String bairro,
            com.empresa.comissao.domain.enums.StatusCliente status) {
        org.springframework.data.jpa.domain.Specification<Cliente> spec = com.empresa.comissao.repository.specification.ClienteSpecification
                .comFiltros(termo, cidade, bairro, status);

        return clienteRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Deprecated or redirect
    public List<ClienteResponse> listarTodos() {
        return listar(null, null, null, null);
    }

    private void updateEntity(Cliente c, ClienteRequest r) {
        c.setRazaoSocial(r.getRazaoSocial());
        c.setNomeFantasia(r.getNomeFantasia());
        c.setCnpj(r.getCnpj());
        c.setEmail(r.getEmail());
        c.setContato(r.getContato());
        if (r.getStatus() != null)
            c.setStatus(r.getStatus());

        c.setLogradouro(r.getLogradouro());
        c.setNumero(r.getNumero());
        c.setComplemento(r.getComplemento());
        c.setBairro(r.getBairro());
        c.setCidade(r.getCidade());
        c.setEstado(r.getEstado());
        c.setCep(r.getCep());

        if (r.getEndereco() != null)
            c.setEndereco(r.getEndereco());
    }

    private ClienteResponse mapToResponse(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .razaoSocial(c.getRazaoSocial())
                .nomeFantasia(c.getNomeFantasia())
                .cnpj(c.getCnpj())
                .contato(c.getContato())
                .email(c.getEmail())
                .status(c.getStatus())
                .logradouro(c.getLogradouro())
                .numero(c.getNumero())
                .complemento(c.getComplemento())
                .bairro(c.getBairro())
                .cidade(c.getCidade())
                .estado(c.getEstado())
                .cep(c.getCep())
                .build();
    }
}
