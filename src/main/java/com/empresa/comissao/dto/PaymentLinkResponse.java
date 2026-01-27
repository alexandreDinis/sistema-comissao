package com.empresa.comissao.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentLinkResponse {
    private String paymentId;
    private String preferenceId;
    private String url;
    private String qrCodePix;
    private String qrCodeImageUrl;
}
