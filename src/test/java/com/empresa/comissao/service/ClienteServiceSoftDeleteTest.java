
package com.empresa.comissao.service;

import com.empresa.comissao.config.TenantContext;
import com.empresa.comissao.domain.entity.Cliente;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.enums.StatusCliente;
import com.empresa.comissao.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceSoftDeleteTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TenantContext tenantContext; // Not directly used as static method is mocked

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private final Long CLIENTE_ID = 1L;
    private final Long TENANT_ID = 100L;

    @BeforeEach
    void setUp() {
        Empresa empresa = new Empresa();
        empresa.setId(TENANT_ID);

        cliente = new Cliente();
        cliente.setId(CLIENTE_ID);
        cliente.setEmpresa(empresa);
        cliente.setStatus(StatusCliente.ATIVO);
        cliente.setDeletedAt(null);
    }

    @Test
    void softDelete_ShouldMarkAsDeleted_WhenNotDeleted() {
        try (MockedStatic<TenantContext> mockedTenant = mockStatic(TenantContext.class)) {
            mockedTenant.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));

            clienteService.deletar(CLIENTE_ID);

            assertNotNull(cliente.getDeletedAt());
            assertEquals(StatusCliente.INATIVO, cliente.getStatus());
            verify(clienteRepository).save(cliente);
            verify(clienteRepository, never()).delete(any(Cliente.class));
        }
    }

    @Test
    void softDelete_ShouldBeIdempotent_WhenAlreadyDeleted() {
        try (MockedStatic<TenantContext> mockedTenant = mockStatic(TenantContext.class)) {
            mockedTenant.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            cliente.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente));

            clienteService.deletar(CLIENTE_ID);

            verify(clienteRepository, never()).save(any(Cliente.class));
            verify(clienteRepository, never()).delete(any(Cliente.class));
        }
    }

    @Test
    void listarTodos_ShouldExcludeDeleted() {
        try (MockedStatic<TenantContext> mockedTenant = mockStatic(TenantContext.class)) {
            mockedTenant.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            when(clienteRepository.findByEmpresaIdAndDeletedAtIsNull(TENANT_ID))
                    .thenReturn(Collections.singletonList(cliente));

            var result = clienteService.listarTodos();

            assertEquals(1, result.size());
            verify(clienteRepository).findByEmpresaIdAndDeletedAtIsNull(TENANT_ID);
        }
    }

    @Test
    void listarSync_ShouldIncludeDeleted_WhenSinceProvided() {
        try (MockedStatic<TenantContext> mockedTenant = mockStatic(TenantContext.class)) {
            mockedTenant.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            LocalDateTime since = LocalDateTime.now().minusDays(1);

            // Simulating a deleted client returned by sync query
            Cliente deletedClient = new Cliente();
            deletedClient.setId(2L);
            deletedClient.setDeletedAt(LocalDateTime.now());

            when(clienteRepository.findSyncData(since, TENANT_ID))
                    .thenReturn(List.of(cliente, deletedClient));

            var result = clienteService.listarSync(since);

            assertEquals(2, result.size());
            verify(clienteRepository).findSyncData(since, TENANT_ID);
        }
    }

    @Test
    void listarSync_ShouldExcludeDeleted_WhenSinceNull() {
        try (MockedStatic<TenantContext> mockedTenant = mockStatic(TenantContext.class)) {
            mockedTenant.when(TenantContext::getCurrentTenant).thenReturn(TENANT_ID);

            when(clienteRepository.findByEmpresaIdAndDeletedAtIsNull(TENANT_ID))
                    .thenReturn(Collections.singletonList(cliente));

            var result = clienteService.listarSync(null);

            assertEquals(1, result.size());
            verify(clienteRepository).findByEmpresaIdAndDeletedAtIsNull(TENANT_ID);
        }
    }
}
