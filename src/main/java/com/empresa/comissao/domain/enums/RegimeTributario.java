package com.empresa.comissao.domain.enums;

/**
 * Regimes tributários suportados pelo sistema.
 * Cada regime tem características próprias de tributação.
 */
public enum RegimeTributario {
    SIMPLES_NACIONAL("Simples Nacional"),
    LUCRO_PRESUMIDO("Lucro Presumido"),
    LUCRO_REAL("Lucro Real"),
    MEI("MEI - Microempreendedor Individual");

    private final String descricao;

    RegimeTributario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
