package dev.rgcb.scholar.application;

import java.time.Duration;
import java.util.Objects;

/** Timing policy for crash-recovery snapshots. */
public record RecoveryPolicy(Duration interval) {
    public static final RecoveryPolicy DEFAULT = new RecoveryPolicy(Duration.ofSeconds(30));

    public RecoveryPolicy {
        interval = Objects.requireNonNull(interval, "interval");
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("Recovery interval must be positive.");
        }
    }
}
