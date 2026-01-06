package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Faturamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FaturamentoRepositoryTest {

//    @Autowired
//    private FaturamentoRepository faturamentoRepository;
//
//    @Test
//    void deveBuscarFaturamentosPorPeriodo() {
//        Faturamento f = Faturamento.builder()
//                .ciente("Teste")
//                .valor(BigDecimal.valueOf(100))
//                .dataFaturamento(LocalDate.now())
//                .build();
//        faturamentoRepository.save(f);
//
//        List<Faturamento> result = faturamentoRepository.findByDataFaturamentoBetween(
//                LocalDate.now().minusDays(1),
//                LocalDate.now().plusDays(1));
//
//        assertThat(result).isNotNull().hasSize(1);
//    }
}
