package com.empresa.comissao.repository.spec;

import com.empresa.comissao.domain.entity.OrdemServico;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.StatusOrdemServico;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrdemServicoSpecification {

    public static Specification<OrdemServico> withFilter(
            Long empresaId,
            User usuario, // If null, user is Admin
            String statusStr,
            String search,
            LocalDate date,
            Boolean atrasado) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tenant Filter (Mandatory)
            predicates.add(cb.equal(root.get("empresa").get("id"), empresaId));

            // ⚠️ FIX N+1: FETCH JOINS
            // Only apply fetches for select queries, not count queries
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("cliente", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Fetch<Object, Object> v = root.fetch("veiculos",
                        jakarta.persistence.criteria.JoinType.LEFT);
                v.fetch("pecas", jakarta.persistence.criteria.JoinType.LEFT);

                // Essential for correct pagination with collection fetches
                query.distinct(true);
            }

            // 2. User Scope Filter (if not Admin)
            if (usuario != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), usuario.getId()));
            }

            // 3. Status Filter (Tab)
            if (statusStr != null && !statusStr.isEmpty()) {
                if ("INICIADAS".equalsIgnoreCase(statusStr)) {
                    predicates.add(root.get("status").in(StatusOrdemServico.ABERTA, StatusOrdemServico.EM_EXECUCAO));
                } else if ("ATRASADAS".equalsIgnoreCase(statusStr) || Boolean.TRUE.equals(atrasado)) {
                    // Logic handled below in 'Atrasadas' block, but if passed as status string:
                    // Using common logic below to avoid duplication
                } else {
                    try {
                        StatusOrdemServico status = StatusOrdemServico.valueOf(statusStr);
                        predicates.add(cb.equal(root.get("status"), status));
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid status
                    }
                }
            }

            // 4. 'Atrasadas' Logic
            // Atrasado = (ABERTA or EM_EXECUCAO) AND DataVencimento < Now
            if ("ATRASADAS".equalsIgnoreCase(statusStr) || Boolean.TRUE.equals(atrasado)) {
                Predicate statusPredicate = root.get("status").in(StatusOrdemServico.ABERTA,
                        StatusOrdemServico.EM_EXECUCAO);
                Predicate datePredicate = cb.lessThan(root.get("dataVencimento"), LocalDate.now());
                predicates.add(cb.and(statusPredicate, datePredicate));
            }

            // 5. Search Filter (Client Name/RazaoSocial or LocalID or ID)
            if (search != null && !search.isEmpty()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                Predicate nomeFantasia = cb.like(cb.lower(root.get("cliente").get("nomeFantasia")), likePattern);
                Predicate razaoSocial = cb.like(cb.lower(root.get("cliente").get("razaoSocial")), likePattern);

                // Try to parse as ID if numeric
                Predicate idPredicate = null;
                if (search.matches("\\d+")) {
                    idPredicate = cb.equal(root.get("id"), Long.parseLong(search));
                }

                if (idPredicate != null) {
                    predicates.add(cb.or(nomeFantasia, razaoSocial, idPredicate));
                } else {
                    predicates.add(cb.or(nomeFantasia, razaoSocial));
                }
            }

            // 6. Date Filter
            if (date != null) {
                predicates.add(cb.equal(root.get("data"), date));
            }

            // Soft delete check
            predicates.add(cb.isNull(root.get("deletedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
