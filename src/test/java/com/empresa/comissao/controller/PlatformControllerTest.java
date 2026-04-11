package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FeatureRepository;
import com.empresa.comissao.repository.UserRepository;
import com.empresa.comissao.repository.LicencaRepository;
import com.empresa.comissao.security.AuthPrincipal;
import com.empresa.comissao.security.AuthVersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlatformControllerTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FeatureRepository featureRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthVersionService authVersionService;
    @Mock
    private LicencaRepository licencaRepository;

    @InjectMocks
    private PlatformController platformController;

    @Test
    public void toggleTenantStatus_ShouldUpdateAtivoAndStatusAndInvalidateCache() {
        // Arrange
        Long userId = 1L;
        AuthPrincipal authPrincipal = new AuthPrincipal(userId, "admin@test.com", null);
        
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setRole(Role.SUPER_ADMIN);
        
        Long tenantId = 100L;
        Empresa empresa = new Empresa();
        empresa.setId(tenantId);
        empresa.setAtivo(false);
        empresa.setStatus(StatusEmpresa.BLOQUEADA);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(empresaRepository.findById(tenantId)).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ResponseEntity<Empresa> response = platformController.toggleTenantStatus(tenantId, authPrincipal);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isAtivo());
        assertEquals(StatusEmpresa.ATIVA, response.getBody().getStatus());
        
        verify(empresaRepository, times(1)).save(empresa);
        verify(authVersionService, times(1)).incrementTenantVersion(tenantId);
    }
    
    @Test
    public void toggleTenantStatus_ShouldBlockWhenActive() {
        // Arrange
        Long userId = 1L;
        AuthPrincipal authPrincipal = new AuthPrincipal(userId, "admin@test.com", null);
        
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setRole(Role.SUPER_ADMIN);
        
        Long tenantId = 100L;
        Empresa empresa = new Empresa();
        empresa.setId(tenantId);
        empresa.setAtivo(true);
        empresa.setStatus(StatusEmpresa.ATIVA);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(empresaRepository.findById(tenantId)).thenReturn(Optional.of(empresa));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ResponseEntity<Empresa> response = platformController.toggleTenantStatus(tenantId, authPrincipal);

        // Assert
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isAtivo());
        assertEquals(StatusEmpresa.BLOQUEADA, response.getBody().getStatus());
        
        verify(empresaRepository, times(1)).save(empresa);
        verify(authVersionService, times(1)).incrementTenantVersion(tenantId);
    }
}
