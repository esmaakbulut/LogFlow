package logflow;

import java.time.Instant;

public record FailedRecord(
        String rawInput,
        String stageName,
        String exception,
        Instant timestamp
) {
}