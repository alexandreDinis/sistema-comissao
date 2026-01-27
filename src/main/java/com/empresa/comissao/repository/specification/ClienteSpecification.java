package com.empresa.comissao.repository.specification;

import com.empresa.comissao.domain.entity.Cliente;
import com.empresa.comissao.domain.enums.StatusCliente;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class ClienteSpecification {

    public static Specification<Cliente> comFiltros(String termo, String cidade, String bairro, StatusCliente status) {
        return (root, query, criteriaBuilder) -> {
            Specification<Cliente> spec = Specification.where(null);

            if (StringUtils.hasText(termo)) {
                String likeTerm = "%" + termo.toLowerCase() + "%";
                spec = spec.and((root2, query2, cb) -> cb.or(
                        cb.like(cb.lower(root2.get("razaoSocial")), likeTerm),
                        cb.like(cb.lower(root2.get("nomeFantasia")), likeTerm),
                        cb.like(cb.lower(root2.get("cnpj")), likeTerm)));
            }

            if (StringUtils.hasText(cidade)) {
                spec = spec.and((root2, query2, cb) -> cb.equal(cb.lower(root2.get("cidade")), cidade.toLowerCase()));
            }

            if (StringUtils.hasText(bairro)) {
                spec = spec.and((root2, query2, cb) -> cb.equal(cb.lower(root2.get("bairro")), bairro.toLowerCase()));
            }

            if (status != null) {
                spec = spec.and((root2, query2, cb) -> cb.equal(root2.get("status"), status));
            }

            return spec.toPredicate(root, query, criteriaBuilder);
        };
    }

    public static Specification<Cliente> porEmpresa(com.empresa.comissao.domain.entity.Empresa empresa) {
        return (root, query, cb) -> cb.equal(root.get("empresa"), empresa);
    }
}
