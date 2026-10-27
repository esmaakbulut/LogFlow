package logflow;

import java.time.Instant;
import java.util.Set;

public class ValidationStage implements Stage<LogRecord, LogRecord> {

    private final Set<String> requiredFields;
    private final long minBytes;
    private final long maxBytes;
    private final Instant minTimestamp;
    private final Instant maxTimestamp;
    private final int maxLineLength;
    private final boolean asciiUserAgentOnly;

    public ValidationStage(
            Set<String> requiredFields,
            long minBytes,
            long maxBytes,
            Instant minTimestamp,
            Instant maxTimestamp,
            int maxLineLength,
            boolean asciiUserAgentOnly) {

        this.requiredFields = requiredFields;
        this.minBytes = minBytes;
        this.maxBytes = maxBytes;
        this.minTimestamp = minTimestamp;
        this.maxTimestamp = maxTimestamp;
        this.maxLineLength = maxLineLength;
        this.asciiUserAgentOnly = asciiUserAgentOnly;
    }

    @Override
    public void process(
            LogRecord record,
            Emitter<LogRecord> out) throws StageException {

        validateRequiredFields(record);
        validateBytes(record);
        validateTimestamp(record);
        validateIp(record);
        validateLineLength(record);
        validateUserAgent(record);

        out.emit(record);
    }

    private void validateRequiredFields(LogRecord record)
            throws StageException {

        if (requiredFields.contains("clientIp")
                && (record.clientIp() == null || record.clientIp().isBlank())) {
            throw new StageException(
                    "Missing required field: clientIp"
            );
        }

        if (requiredFields.contains("path")
                && (record.path() == null || record.path().isBlank())) {
            throw new StageException(
                    "Missing required field: path"
            );
        }
    }

    private void validateBytes(LogRecord record)
            throws StageException {

        if (record.bytes() < minBytes
                || record.bytes() > maxBytes) {

            throw new StageException(
                    "Bytes value out of range: "
                            + record.bytes()
            );
        }
    }

    private void validateTimestamp(LogRecord record)
            throws StageException {

        if (record.timestamp().isBefore(minTimestamp)
                || record.timestamp().isAfter(maxTimestamp)) {

            throw new StageException(
                    "Timestamp out of allowed range: "
                            + record.timestamp()
            );
        }
    }

    private void validateIp(LogRecord record)
            throws StageException {

        String ip = record.clientIp();

        if (!ip.matches(
                "^((25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.){3}"
                        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])$")) {

            throw new StageException(
                    "Invalid IPv4 address: " + ip
            );
        }
    }

    private void validateLineLength(LogRecord record)
            throws StageException {

        if (record.raw() != null
                && record.raw().length() > maxLineLength) {

            throw new StageException(
                    "Log line is too long: "
                            + record.raw().length()
            );
        }
    }

    private void validateUserAgent(LogRecord record)
            throws StageException {

        if (!asciiUserAgentOnly) {
            return;
        }

        String userAgent = record.userAgent();

        for (int i = 0; i < userAgent.length(); i++) {

            if (userAgent.charAt(i) > 127) {

                throw new StageException(
                        "User-Agent contains non-ASCII characters"
                );
            }
        }
    }
}