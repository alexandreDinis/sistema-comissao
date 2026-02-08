package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class SyncStatusResponse {
    private Instant serverTime;
    private Instant clientesUpdatedAtMax;
    private Instant osUpdatedAtMax;
}
