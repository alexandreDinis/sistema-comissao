package com.empresa.comissao.domain.enums;

public enum CategoriaDespesa {
    // Operacionais
    ALIMENTACAO("Alimentação"),
    COMBUSTIVEL("Combustível"),
    FERRAMENTAS("Ferramentas"),
    MATERIAL_ESCRITORIO("Material de Escritório"),

    // Infraestrutura
    ALUGUEL("Aluguel"),
    ENERGIA_AGUA("Energia/Água"),
    INTERNET_TELEFONE("Internet/Telefone"),
    MANUTENCAO("Manutenção"),

    // Pessoal
    SALARIOS("Salários"),
    ADIANTAMENTOS("Adiantamentos"),
    COMISSOES_PAGAS("Comissões Pagas"),
    PROLABORE("Pró-Labore"),
    BENEFICIOS("Benefícios"),

    // Marketing/Vendas
    MARKETING("Marketing"),
    TAXAS_BANCARIAS("Taxas Bancárias"),

    // Fiscal
    IMPOSTOS("Impostos"),
    CONTABILIDADE("Contabilidade"),

    // Terceiros
    SERVICOS_TERCEIROS("Serviços de Terceiros"),

    // Genérico
    DIVERSOS("Diversos"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaDespesa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
