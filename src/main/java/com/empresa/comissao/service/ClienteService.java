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
import com.empresa.comissao.domain.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse criar(ClienteRequest request, com.empresa.comissao.domain.entity.User usuario) {
        com.empresa.comissao.domain.enums.TipoPessoa tipo = request.getTipoPessoa() != null
                ? request.getTipoPessoa()
                : com.empresa.comissao.domain.enums.TipoPessoa.JURIDICA;

        if (tipo == com.empresa.comissao.domain.enums.TipoPessoa.FISICA) {
            com.empresa.comissao.validation.ValidadorDocumento.validarCpf(request.getCpf());
        } else {
            com.empresa.comissao.validation.ValidadorDocumento.validarCnpj(request.getCnpj());
        }

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

        validarAcesso(cliente);

        com.empresa.comissao.domain.enums.TipoPessoa tipo = request.getTipoPessoa() != null
                ? request.getTipoPessoa()
                : com.empresa.comissao.domain.enums.TipoPessoa.JURIDICA;

        if (tipo == com.empresa.comissao.domain.enums.TipoPessoa.FISICA) {
            com.empresa.comissao.validation.ValidadorDocumento.validarCpf(request.getCpf());
        } else {
            com.empresa.comissao.validation.ValidadorDocumento.validarCnpj(request.getCnpj());
        }

        updateEntity(cliente, request);
        cliente = clienteRepository.save(cliente);
        return mapToResponse(cliente);
    }

    public void deletar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));

        validarAcesso(cliente);

        clienteRepository.delete(cliente);
    }

    public ClienteResponse buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));
        validarAcesso(cliente);
        return mapToResponse(cliente);
    }

    public List<ClienteResponse> listar(String termo, String cidade, String bairro,
            com.empresa.comissao.domain.enums.StatusCliente status) {
        org.springframework.data.jpa.domain.Specification<Cliente> spec = com.empresa.comissao.repository.specification.ClienteSpecification
                .comFiltros(termo, cidade, bairro, status);

        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getEmpresa() != null) {
            spec = spec.and(com.empresa.comissao.repository.specification.ClienteSpecification
                    .porEmpresa(currentUser.getEmpresa()));
        } else {
            // If no tenant context, return empty list to prevent leak
            return java.util.Collections.emptyList();
        }

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
        c.setCpf(r.getCpf());
        c.setTipoPessoa(
                r.getTipoPessoa() != null ? r.getTipoPessoa() : com.empresa.comissao.domain.enums.TipoPessoa.JURIDICA);
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
                .nomeFantasia(c.getNomeFantasia())
                .cnpj(c.getCnpj())
                .cpf(c.getCpf())
                .tipoPessoa(c.getTipoPessoa())
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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    private void validarAcesso(Cliente cliente) {
        User user = getCurrentUser();
        if (user != null && user.getEmpresa() != null) {
            if (cliente.getEmpresa() == null || !cliente.getEmpresa().getId().equals(user.getEmpresa().getId())) {
                throw new jakarta.persistence.EntityNotFoundException("Cliente não encontrado");
            }
        }
    }
}
