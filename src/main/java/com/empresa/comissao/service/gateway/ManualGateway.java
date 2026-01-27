package com.empresa.comissao.service.gateway;

import com.empresa.comissao.domain.entity.FaturaTenant;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.dto.PaymentLinkResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ManualGateway implements IPaymentGateway {

    @Override
    public IPaymentGateway configure(String accessToken) {
        return this;
    }

    @Override
    public PaymentLinkResponse criarLinkPagamento(FaturaTenant fatura, Licenca licenca) {
        // Mock implementation
        return PaymentLinkResponse.builder()
                .paymentId(UUID.randomUUID().toString())
                .preferenceId("PREF-" + fatura.getId())
                .url("https://sistema.com/pay/" + fatura.getId()) // Fake URL
                .qrCodePix("00020126580014br.gov.bcb.pix...")
                .qrCodeImageUrl("https://chart.googleapis.com/chart?chs=150x150&cht=qr&chl=pix")
                .build();
    }
}
