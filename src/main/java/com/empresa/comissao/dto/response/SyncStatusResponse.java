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
    private Boolean clientesUpdated;
    private Boolean osUpdated;
    private Boolean tiposPecaUpdated;
    private Boolean usersUpdated;
    private Boolean comissoesUpdated;
    private Long lastTenantVersion;
}
