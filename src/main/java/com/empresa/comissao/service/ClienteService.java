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
    public ClienteResponse criar(ClienteRequest request) {
        // Idempotency Check (Correlation ID)
        if (request.getCorrelationId() != null && !request.getCorrelationId().isBlank()) {
            java.util.Optional<Cliente> existing = clienteRepository.findByCorrelationId(request.getCorrelationId());
            if (existing.isPresent()) {
                // Return existing to preventing duplicates
                return mapToResponse(existing.get());
            }
        }

        validarDocumentos(request);

        Cliente cliente = new Cliente();
        updateEntity(cliente, request);
        cliente.setEmpresa(getEmpresaAutenticada());
        cliente = clienteRepository.save(cliente);
        return mapToResponse(cliente);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));

        validarAcesso(cliente);
        validarDocumentos(request);

        updateEntity(cliente, request);
        cliente = clienteRepository.save(cliente);
        return mapToResponse(cliente);
    }

    public void deletar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cliente não encontrado"));

        validarAcesso(cliente);

        if (cliente.getDeletedAt() != null) {
            return; // Idempotent
        }

        cliente.setDeletedAt(java.time.LocalDateTime.now());
        cliente.setUpdatedAt(java.time.LocalDateTime.now());
        cliente.setStatus(com.empresa.comissao.domain.enums.StatusCliente.INATIVO);

        clienteRepository.save(cliente);
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
            com.empresa.comissao.domain.entity.Empresa empresaRef = new com.empresa.comissao.domain.entity.Empresa();
            empresaRef.setId(tenantId);
            spec = spec.and(com.empresa.comissao.repository.specification.ClienteSpecification
                    .porEmpresa(empresaRef));
        } else {
            return java.util.Collections.emptyList();
        }

        return clienteRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Deprecated or redirect
    public List<ClienteResponse> listarTodos() {
        // Listar sem filtros, apenas não deletados do tenant
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null)
            return java.util.Collections.emptyList();

        return clienteRepository.findByEmpresaIdAndDeletedAtIsNull(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ClienteResponse> listarSync(java.time.LocalDateTime since) {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return java.util.Collections.emptyList();
        }

        List<Cliente> clientes;
        if (since != null) {
            // Sync traz tudo que mudou, inclusive deletados (para o app remover via
            // localId/ID)
            clientes = clienteRepository.findSyncData(since, tenantId);
        } else {
            // Full Sync inicial: não traz deletados
            clientes = clienteRepository.findByEmpresaIdAndDeletedAtIsNull(tenantId);
        }

        return clientes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void updateEntity(Cliente c, ClienteRequest r) {
        c.setRazaoSocial(r.getRazaoSocial());
        c.setNomeFantasia(r.getNomeFantasia());
        c.setCnpj(r.getCnpj() != null && !r.getCnpj().isBlank() ? r.getCnpj() : null);
        c.setCpf(r.getCpf() != null && !r.getCpf().isBlank() ? r.getCpf() : null);
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

        if (r.getCorrelationId() != null)
            c.setCorrelationId(r.getCorrelationId());
    }

    private ClienteResponse mapToResponse(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .razaoSocial(c.getRazaoSocial())
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
                .correlationId(c.getCorrelationId())
                .deletedAt(
                        c.getDeletedAt() != null ? c.getDeletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                                : null)
                .updatedAt(
                        c.getUpdatedAt() != null ? c.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                                : null)
                .build();
    }

    private void validarDocumentos(ClienteRequest request) {
        com.empresa.comissao.domain.enums.TipoPessoa tipo = request.getTipoPessoa() != null
                ? request.getTipoPessoa()
                : com.empresa.comissao.domain.enums.TipoPessoa.JURIDICA;

        if (tipo == com.empresa.comissao.domain.enums.TipoPessoa.FISICA) {
            if (request.getCpf() != null && !request.getCpf().isBlank()) {
                com.empresa.comissao.validation.ValidadorDocumento.validarCpf(request.getCpf());
            }
        } else {
            if (request.getCnpj() != null && !request.getCnpj().isBlank()) {
                com.empresa.comissao.validation.ValidadorDocumento.validarCnpj(request.getCnpj());
            }
        }
    }

    private com.empresa.comissao.domain.entity.Empresa getEmpresaAutenticada() {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return com.empresa.comissao.domain.entity.Empresa.builder().id(tenantId).build();
        }
        throw new jakarta.persistence.EntityNotFoundException(
                "Usuário não vinculado a uma empresa (TenantContext vazio)");
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
