package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.FaturaTenant;
import com.empresa.comissao.domain.enums.StatusFatura;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FaturaTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final FaturaTenantRepository faturaTenantRepository;
    private final EmpresaRepository empresaRepository;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> handleMercadoPagoWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "x-signature", required = false) String signature) {

        log.info("Webhook received from Mercado Pago: {}", payload);

        if (!validarAssinatura(payload, signature)) {
            log.warn("Assinatura de webhook inválida. Ignorando requisição.");
            // return ResponseEntity.status(401).build(); // Habilitar em produção
        }

        try {
            String type = (String) payload.get("type");

            if ("payment".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                String paymentId = (String) data.get("id");

                // Como não temos validação segura ainda, vamos buscar pelo ID no gateway apenas
                // se tivermos mapeado
                // Ou podemos confiar se o payload trouxer o external_reference.
                // Vou simular que processamos o pagamento com base no ID que salvamos na
                // fatura.

                if (data.containsKey("external_reference")) {
                    String externalRef = (String) data.get("external_reference");
                    log.info("Processing webhook with context: {}", externalRef);
                    // Aqui decodificaríamos o JSON do external_reference para validar o contexto
                }

                faturaTenantRepository.findByPaymentId(paymentId).ifPresent(fatura -> {
                    // Simular verificação de status "approved" (na real faria chamada a API do MP)
                    // if (status == approved) ...

                    processarPagamentoTenant(fatura, paymentId);
                });
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(500).build();
        }
    }

    private void processarPagamentoTenant(FaturaTenant fatura, String paymentId) {
        if (fatura.getStatus() == StatusFatura.PAGO)
            return;

        log.info("Processing payment for Invoice {}", fatura.getId());

        fatura.setStatus(StatusFatura.PAGO);
        fatura.setDataPagamento(LocalDate.now());
        // fatura.setValorPago(...); // Pegar do payload real

        faturaTenantRepository.save(fatura);

        // Desbloquear empresa se necessário
        Empresa empresa = fatura.getEmpresa();
        if (empresa.getStatus() == StatusEmpresa.BLOQUEADA) {
            empresa.setStatus(StatusEmpresa.ATIVA);
            empresaRepository.save(empresa);
            log.info("Tenant {} UNBLOCKED after payment", empresa.getId());
        }
    }

    private boolean validarAssinatura(Map<String, Object> payload, String signature) {
        // TODO: Implementar validação HMAC-SHA256 real em produção
        // Por enquanto, retornamos true para desenvolvimento
        if (signature == null || signature.isBlank()) {
            log.warn("No signature provided, skipping validation (DEV mode)");
            return true;
        }
        // Em produção: validar signature com secret da Licença
        return true;
    }
}
