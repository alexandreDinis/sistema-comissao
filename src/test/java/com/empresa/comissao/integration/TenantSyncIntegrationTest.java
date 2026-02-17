package com.empresa.comissao.integration;

import com.empresa.comissao.config.TenantContext;
import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.dto.request.OrdemServicoRequest;
import com.empresa.comissao.repository.*;
import com.empresa.comissao.service.OrdemServicoService;
import com.empresa.comissao.service.TenantVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@Transactional // Rollback after each test
class TenantSyncIntegrationTest {

    @Autowired
    private OrdemServicoService osService;

    @Autowired
    private TenantVersionService tenantVersionService;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UserRepository userRepository;

    private Empresa empresa;
    private User usuario;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        // Setup Tenant
        empresa = new Empresa();
        empresa.setNome("Test Company");
        empresa.setCnpj("00000000000100");
        empresa.setEmail("test@company.com");
        empresa.setTenantVersion(1L);
        empresa = empresaRepository.save(empresa);

        // Setup User
        usuario = new User();
        usuario.setEmail("user@test.com");
        usuario.setPassword("password");
        usuario.setEmpresa(empresa);
        usuario = userRepository.save(usuario);

        // Setup Cliente
        cliente = new Cliente();
        cliente.setRazaoSocial("Test Client");
        cliente.setEmpresa(empresa);
        cliente = clienteRepository.save(cliente);
    }

    @Test
    void shouldIncrementTenantVersion_WhenCreatingOS() {
        try (MockedStatic<TenantContext> mockedTenant = mockStatic(TenantContext.class)) {
            mockedTenant.when(TenantContext::getCurrentTenant).thenReturn(empresa.getId());

            // Mock Auth Context? OrdemServicoService.getUserAutenticado might need it.
            // For integration tests, we might need to mock SecurityContextHolder too if the
            // service uses it.
            // But let's check if we can bypass or set it up.
            // Assuming for now simple execution.
            // Actually, OrdemServicoService calls getUserAutenticado which checks
            // SecurityContext.
            // We need to verify if we can stub that or if the test will fail.
            // For now, let's try the simplest path. If it fails, I'll add SecurityContext
            // mocking.

            // Initial Version
            Long initialVersion = tenantVersionService.getCurrentVersion(empresa.getId());
            assertThat(initialVersion).isNotNull();

            // Action: Create OS
            OrdemServicoRequest request = new OrdemServicoRequest();
            request.setClienteId(cliente.getId());
            request.setData(LocalDate.now());
            request.setUsuarioId(usuario.getId()); // Explicitly set user to avoid auth context requirement?

            // OrdemServicoService.criarOS gets "getEmpresaAutenticada" from TenantContext
            // (Already mocked)
            // And "getUserAutenticado" from SecurityContext.
            // If usuarioId is provided, it might skip using the authenticated user for the
            // entity relation,
            // BUT lines 316-318 in service:
            // } else if (usuario != null) { os.setUsuario(usuario); }
            // "usuario" comes from getUserAutenticado().
            // If getUserAutenticado returns null, it's fine as long as it doesn't throw
            // exception.
            // The service method catches null auth?
            // "if (auth != null && ...)" -> returns null.
            // So providing usuarioId in request should be enough.

            osService.criarOS(request);

            // Assert
            Long newVersion = tenantVersionService.getCurrentVersion(empresa.getId());
            assertThat(newVersion).isGreaterThan(initialVersion);
        }
    }
}
