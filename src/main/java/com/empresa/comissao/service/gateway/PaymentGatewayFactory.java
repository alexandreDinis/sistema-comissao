package com.empresa.comissao.service.gateway;

import com.empresa.comissao.domain.entity.Licenca;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final ApplicationContext context;

    public IPaymentGateway getGateway(Licenca licenca) {
        String gatewayName = licenca.getGatewayPagamento();
        if (gatewayName == null) {
            gatewayName = "MANUAL";
        }

        switch (gatewayName) {
            case "MERCADO_PAGO":
                return context.getBean(MercadoPagoGateway.class)
                        .configure(licenca.getGatewayAccessToken());

            // case "ASAAS": return ...

            case "MANUAL":
            default:
                return context.getBean(ManualGateway.class)
                        .configure(null);
        }
    }
}
