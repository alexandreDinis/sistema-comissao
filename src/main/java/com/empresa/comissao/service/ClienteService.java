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

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId != null) {
            // Specification needs to be updated to support filtering by ID without loading
            // Empresa entity if possible,
            // but for now let's assume we can't easily change Specification logic without
            // seeing it.
            // Using a dummy/reference Empresa object or just updating the spec usage?
            // Spec `porEmpresa` likely expects an Empresa object.
            com.empresa.comissao.domain.entity.Empresa empresaRef = new com.empresa.comissao.domain.entity.Empresa();
            empresaRef.setId(tenantId);
            spec = spec.and(com.empresa.comissao.repository.specification.ClienteSpecification
                    .porEmpresa(empresaRef));
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

    public List<ClienteResponse> listarSync(java.time.LocalDateTime since) {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return java.util.Collections.emptyList();
        }

        List<Cliente> clientes;
        if (since != null) {
            clientes = clienteRepository.findSyncData(since, tenantId);
        } else {
            clientes = clienteRepository.findAllByEmpresaId(tenantId);
        }

        return clientes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                .localId(c.getLocalId())
                .deletedAt(c.getDeletedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private void validarAcesso(Cliente cliente) {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId != null) {
            if (cliente.getEmpresa() == null || !cliente.getEmpresa().getId().equals(tenantId)) {
                throw new jakarta.persistence.EntityNotFoundException("Cliente não encontrado");
            }
        }
    }
}
