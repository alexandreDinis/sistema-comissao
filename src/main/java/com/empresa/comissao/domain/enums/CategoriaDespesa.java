package com.empresa.comissao.domain.enums;

public enum CategoriaDespesa {
    // 1. CUSTO DOS SERVIÇOS PRESTADOS (CSP)
    MATERIAIS_APLICADOS("Materiais Aplicados"),
    SERVICOS_TERCEIROS("Serviços de Terceiros"),

    // 2. DESPESAS COM PESSOAL
    SALARIOS("Salários"),
    PROLABORE("Pró-Labore"), // Excluded from DRE Operacional
    COMISSOES("Comissões"),
    BENEFICIOS("Benefícios"),
    ENCARGOS_SOCIAIS("Encargos Sociais"),
    ADIANTAMENTOS("Adiantamentos"),

    // 3. DESPESAS ADMINISTRATIVAS
    OCUPACAO("Ocupação (Aluguel/Cond)"),
    UTILIDADES("Utilidades (Luz/Água/Tel)"),
    MANUTENCAO_PREDIAL("Manutenção Predial"),
    MATERIAL_USO_CONSUMO("Material de Uso e Consumo"),
    SERVICOS_PROFISSIONAIS("Serviços Profissionais (Cont/Jur)"),
    DIVERSOS("Despesas Diversas"),

    // 4. DESPESAS COMERCIAIS
    MARKETING("Marketing e Publicidade"),
    VIAGENS_REPRESENTACAO("Viagens e Representação"),
    COMBUSTIVEL("Combustível"),

    // 5. DESPESAS FINANCEIRAS
    TARIFAS_BANCARIAS("Tarifas Bancárias"),
    JUROS_PASSIVOS("Juros Passivos"),

    // 6. DESPESAS TRIBUTÁRIAS
    IMPOSTOS_SOBRE_VENDA("Impostos sobre Venda"), // Excluded from DRE (Calculated)
    TAXAS_DIVERSAS("Taxas e Licenças"),

    // LEGACY / MIGRATION (Manter temporariamente ou mapear se necessário)
    OUTROS("Outros");

    private final String descricao;

    CategoriaDespesa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
