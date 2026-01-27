package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.domain.entity.PlanoLicenca;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusLicenca;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.LicencaRepository;
import com.empresa.comissao.repository.PlanoLicencaRepository;
import com.empresa.comissao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicencaService {

    private final LicencaRepository licencaRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanoLicencaRepository planoLicencaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Criar nova licença (revendedor) com usuário admin
     */
    @Transactional
    public Licenca criarLicenca(Licenca licenca, Long planoId, String senhaAdmin) {
        if (licencaRepository.existsByCnpj(licenca.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        if (userRepository.existsByEmail(licenca.getEmail())) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema");
        }

        PlanoLicenca plano = planoLicencaRepository.findById(planoId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        configurarLicencaComPlano(licenca, plano);
        licenca.setStatus(StatusLicenca.ATIVA);
        licenca.setDataAtivacao(LocalDate.now());

        Licenca savedLicenca = licencaRepository.save(licenca);
        log.info("Creating new License: {}", licenca.getRazaoSocial());

        // Criar usuário admin para o revendedor
        User adminUser = User.builder()
                .email(licenca.getEmail())
                .password(passwordEncoder.encode(senhaAdmin))
                .role(Role.ADMIN_LICENCA)
                .licenca(savedLicenca)
                .empresa(null) // Revendedor não tem empresa própria
                .active(true)
                .mustChangePassword(true) // Força troca de senha no primeiro login
                .build();

        userRepository.save(adminUser);
        log.info("Created admin user for License: {} -> {}", licenca.getRazaoSocial(), licenca.getEmail());

        return savedLicenca;
    }

    private void configurarLicencaComPlano(Licenca licenca, PlanoLicenca plano) {
        licenca.setPlanoTipo(plano.getNome());
        licenca.setValorMensalidade(plano.getValorMensalidade());
        licenca.setValorPorTenant(plano.getValorPorTenant());
        licenca.setLimiteTenants(plano.getLimiteTenants());
    }

    /**
     * Verificar se revendedor pode adicionar mais tenants
     */
    public boolean podeAdicionarTenant(Long licencaId) {
        Licenca licenca = licencaRepository.findById(licencaId)
                .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));

        if (licenca.getStatus() != StatusLicenca.ATIVA) {
            return false;
        }

        if (licenca.getLimiteTenants() == null) {
            return true; // Ilimitado
        }

        long tenantsAtivos = empresaRepository.countByLicencaIdAndStatus(licencaId, StatusEmpresa.ATIVA);

        return tenantsAtivos < licenca.getLimiteTenants();
    }

    @Transactional
    public void suspenderLicenca(Long licencaId, String motivo) {
        Licenca licenca = licencaRepository.findById(licencaId)
                .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));

        licenca.setStatus(StatusLicenca.SUSPENSA);
        licenca.setDataSuspensao(LocalDate.now());
        licenca.setMotivoSuspensao(motivo);
        licencaRepository.save(licenca);

        // Bloquear todos os tenants dessa licença
        List<Empresa> tenants = empresaRepository.findByLicencaId(licencaId);
        tenants.forEach(empresa -> {
            empresa.setStatus(StatusEmpresa.BLOQUEADA);
            empresaRepository.save(empresa);
        });

        log.warn("Licença {} suspensa: {}", licencaId, motivo);
    }

    public Page<Licenca> listarLicencas(Pageable pageable) {
        return licencaRepository.findAll(pageable);
    }

    public Licenca buscarPorId(Long id) {
        return licencaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));
    }
}
