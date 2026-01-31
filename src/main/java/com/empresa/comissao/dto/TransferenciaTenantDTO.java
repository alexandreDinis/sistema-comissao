package com.empresa.comissao.dto;

public record TransferenciaTenantDTO(
        Long empresaId,
        Long novaLicencaId,
        String motivo) {
}
