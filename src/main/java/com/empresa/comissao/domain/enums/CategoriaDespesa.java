package com.empresa.comissao.domain.enums;

public enum CategoriaDespesa {
    ALIMENTACAO("Alimentação"),
    COMBUSTIVEL("Combustível"),
    FERRAMENTAS("Ferramentas"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaDespesa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
