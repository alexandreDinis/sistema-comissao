package com.empresa.comissao.aspect;

import com.empresa.comissao.config.TenantContext;
import com.empresa.comissao.service.TenantVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSyncAspect {

    private final TenantVersionService tenantVersionService;

    // Intercept save() on Syncable Repositories
    // We target repositories that handle core business data sent to mobile
    @AfterReturning(pointcut = "execution(* com.empresa.comissao.repository.OrdemServicoRepository.save*(..)) || " +
            "execution(* com.empresa.comissao.repository.ClienteRepository.save*(..)) || " +
            "execution(* com.empresa.comissao.repository.VeiculoServicoRepository.save*(..)) || " +
            "execution(* com.empresa.comissao.repository.PecaServicoRepository.save*(..)) || " +
            "execution(* com.empresa.comissao.repository.TipoPecaRepository.save*(..)) || " +
            "execution(* com.empresa.comissao.repository.UserRepository.save*(..))", returning = "result")
    public void afterSave(JoinPoint joinPoint, Object result) {
        bumpTenant(result);
    }

    // Intercept delete() is harder because we might not have the entity if
    // deleteById is used.
    // However, mostly we use Soft Delete (updates) or delete(entity).
    // If deleteById(id) is used, we only have ID.
    // For now, let's rely on TenantContext for deletions if entity is missing,
    // OR if we return void, we rely on TenantContext.

    @AfterReturning("execution(* com.empresa.comissao.repository.*.delete*(..))")
    public void afterDelete(JoinPoint joinPoint) {
        // Fallback to current tenant in context
        Long currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            tenantVersionService.bump(currentTenant);
            log.debug("Bumped tenant version {} via Aspect (Delete)", currentTenant);
        } else {
            // Try to extract from argument if it is an entity?
            // Too complex for generic catch-all.
        }
    }

    private void bumpTenant(Object entity) {
        if (entity == null)
            return;

        Long tenantId = null;

        // Try to extract tenant ID from known entities via reflection or casting
        // This avoids making all entities implement an interface right now (less
        // invasive)

        if (entity instanceof com.empresa.comissao.domain.entity.Empresa) {
            tenantId = ((com.empresa.comissao.domain.entity.Empresa) entity).getId();
        } else if (entity instanceof com.empresa.comissao.domain.entity.OrdemServico) {
            var os = (com.empresa.comissao.domain.entity.OrdemServico) entity;
            if (os.getEmpresa() != null)
                tenantId = os.getEmpresa().getId();
        } else if (entity instanceof com.empresa.comissao.domain.entity.Cliente) {
            var c = (com.empresa.comissao.domain.entity.Cliente) entity;
            if (c.getEmpresa() != null)
                tenantId = c.getEmpresa().getId();
        } else if (entity instanceof com.empresa.comissao.domain.entity.VeiculoServico) {
            var v = (com.empresa.comissao.domain.entity.VeiculoServico) entity;
            if (v.getOrdemServico() != null && v.getOrdemServico().getEmpresa() != null)
                tenantId = v.getOrdemServico().getEmpresa().getId();
        } else if (entity instanceof com.empresa.comissao.domain.entity.PecaServico) {
            var p = (com.empresa.comissao.domain.entity.PecaServico) entity;
            if (p.getVeiculo() != null && p.getVeiculo().getOrdemServico() != null &&
                    p.getVeiculo().getOrdemServico().getEmpresa() != null)
                tenantId = p.getVeiculo().getOrdemServico().getEmpresa().getId();
        } else if (entity instanceof com.empresa.comissao.domain.entity.User) {
            var u = (com.empresa.comissao.domain.entity.User) entity;
            if (u.getEmpresa() != null)
                tenantId = u.getEmpresa().getId();
        } else if (entity instanceof com.empresa.comissao.domain.entity.TipoPeca) {
            var t = (com.empresa.comissao.domain.entity.TipoPeca) entity;
            if (t.getEmpresa() != null)
                tenantId = t.getEmpresa().getId();
        } else if (entity instanceof Iterable) {
            // Handle saveAll
            for (Object item : (Iterable<?>) entity) {
                bumpTenant(item); // Recursive bump for first item usually enough if all same tenant
                return; // Bump once per batch logic? Or bump once per item?
                // Bump once per batch is enough to invalidate cache/sync.
            }
        } else if (entity instanceof Optional) {
            ((Optional<?>) entity).ifPresent(this::bumpTenant);
        }

        // Fallback to checking TenantContext if entity extraction failed
        if (tenantId == null) {
            tenantId = TenantContext.getCurrentTenant();
        }

        if (tenantId != null) {
            tenantVersionService.bump(tenantId);
            log.trace("Bumped tenant version {} via Aspect (Entity: {})", tenantId, entity.getClass().getSimpleName());
        }
    }
}
