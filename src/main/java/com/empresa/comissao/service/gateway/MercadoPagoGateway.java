package com.empresa.comissao.service.gateway;

import com.empresa.comissao.domain.entity.FaturaTenant;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.dto.PaymentLinkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MercadoPagoGateway implements IPaymentGateway {

    private String accessToken;

    @Override
    public IPaymentGateway configure(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    @Override
    public PaymentLinkResponse criarLinkPagamento(FaturaTenant fatura, Licenca licenca) {
        // Integração pendente da adição do SDK do Mercado Pago
        // Retornando Mock para não quebrar a compilação
        if (accessToken == null) {
            log.warn("Access Token do Mercado Pago não configurado para licença {}", licenca.getId());
        }

        // Placeholder para não quebrar a compilação sem o SDK
        // No SDK real, faríamos:
        // Preference preference = new Preference();
        // preference.setExternalReference("{\"licencaId\":" + licenca.getId() +
        // ",\"tenantId\":" + fatura.getEmpresa().getId() + ",\"faturaId\":" +
        // fatura.getId() + "}");

        return PaymentLinkResponse.builder()
                .url("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=" + fatura.getId())
                .build();
    }
}
