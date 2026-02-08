package com.empresa.comissao.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class SyncUtils {

    private static final long SKEW_SECONDS = 2;
    // Safe epoch to prevent underflow issues (e.g., 2000-01-01)
    private static final LocalDateTime SAFE_EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0);

    /**
     * Normalizes the 'since' parameter by applying a safety skew.
     * This helps preventing data loss due to clock drift or precision differences.
     *
     * @param since The timestamp received from the client
     * @return The adjusted timestamp, or null if input is null
     */
    /**
     * Normalizes the 'since' parameter from Instant (UTC) to Server Local Time.
     * Applies safety skew to prevent data loss.
     */
    public static LocalDateTime normalizeSince(Instant since) {
        if (since == null) {
            return null;
        }
        // Convert Instant (UTC) to Server Local Time
        LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, ZoneId.systemDefault());
        return normalizeSince(sinceLocal);
    }

    public static LocalDateTime normalizeSince(LocalDateTime since) {
        if (since == null) {
            return null;
        }

        LocalDateTime normalized = since.minusSeconds(SKEW_SECONDS);

        // Clamp to safe epoch
        if (normalized.isBefore(SAFE_EPOCH)) {
            return SAFE_EPOCH;
        }

        return normalized;
    }
}
