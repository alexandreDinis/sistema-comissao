package com.empresa.comissao.service.gateway;

import com.empresa.comissao.domain.entity.FaturaTenant;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.dto.PaymentLinkResponse;

public interface IPaymentGateway {
    IPaymentGateway configure(String accessToken);

    PaymentLinkResponse criarLinkPagamento(FaturaTenant fatura, Licenca licenca);
    // Object consultarPagamento(String paymentId); // Generics or specific DTO
    // later
}
