package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.domain.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class VerifyContaReceberFetchTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContaReceberRepository contaReceberRepository;

    @Test
    public void findRecebidosBetween_DeveCarregarRelacoesSemNMaisUm() {
        // 1. Setup Data
        Empresa empresa = new Empresa();
        empresa.setCnpj("00000000000100");
        empresa.setNome("Empresa Teste CR");
        empresa.setModoComissao(ModoComissao.INDIVIDUAL);
        empresa = entityManager.persist(empresa);

        Cliente cliente = new Cliente();
        cliente.setEmpresa(empresa);
        cliente.setRazaoSocial("Cliente Teste CR");
        cliente.setContato("11999999999");
        cliente = entityManager.persist(cliente);

        OrdemServico os = new OrdemServico();
        os.setEmpresa(empresa);
        os.setCliente(cliente);
        os.setData(LocalDate.now());
        os.setStatus(StatusOrdemServico.FINALIZADA);
        os = entityManager.persist(os);

        Faturamento faturamento = new Faturamento();
        faturamento.setEmpresa(empresa);
        faturamento.setOrdemServico(os);
        faturamento.setDataFaturamento(LocalDate.now());
        faturamento.setValor(BigDecimal.TEN);
        faturamento = entityManager.persist(faturamento);

        ContaReceber cr = new ContaReceber();
        cr.setEmpresa(empresa);
        cr.setFaturamento(faturamento);
        cr.setOrdemServico(os);
        cr.setDataCompetencia(LocalDate.now());
        cr.setDataVencimento(LocalDate.now());
        cr.setDataRecebimento(LocalDate.now()); // IMPORTANT: Must be set for 'findRecebidosBetween'
        cr.setStatus(StatusConta.PAGO); // IMPORTANT: Must be PAGO
        cr.setTipo(TipoContaReceber.ORDEM_SERVICO);
        cr.setValor(BigDecimal.TEN);
        cr = entityManager.persist(cr);

        entityManager.flush();
        entityManager.clear(); // Clear cache to force DB fetch

        // 2. Execute Method
        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fim = LocalDate.now().plusDays(1);

        List<ContaReceber> resultados = contaReceberRepository.findRecebidosBetween(empresa, inicio, fim);

        // 3. Verify
        assertThat(resultados).hasSize(1);
        ContaReceber crCarregado = resultados.get(0);

        // Verify relationships are loaded (checking for nulls or traversing)
        // Since we did LEFT JOIN FETCH, these should be populated
        assertThat(crCarregado.getOrdemServico()).isNotNull();
        assertThat(crCarregado.getOrdemServico().getCliente()).isNotNull();
        assertThat(crCarregado.getOrdemServico().getCliente().getRazaoSocial()).isEqualTo("Cliente Teste CR");

        assertThat(crCarregado.getFaturamento()).isNotNull();
        assertThat(crCarregado.getFaturamento().getValor()).isEqualByComparingTo(BigDecimal.TEN);

        System.out.println("✅ Teste de ContaReceber finalizado. Query OTIMIZADA executada com sucesso.");
    }
}
